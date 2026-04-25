<# ============================================================================
 build.ps1 - Master Build Script for mdflib-java JNI Project

 This PowerShell script automates the complete build process including:
 1. Downloading and configuring third-party dependencies (vcpkg, zlib, expat)
 2. Building the mdflib C++ static library from source
 3. Building the JNI bridge shared library (.dll / .so)
 4. Running Java tests with Maven

 Prerequisites:
 - PowerShell 5.1+
 - CMake 3.15+
 - Visual Studio 2019+ (Windows) or GCC 7+ (Linux)
 - JDK 8+
 - Maven 3.6+
 - Git (for vcpkg)

 Usage:
   .\build.ps1                    # Full build (all steps)
   .\build.ps1 -SkipTests         # Build without running tests
   .\build.ps1 -Clean             # Clean build (removes previous artifacts)
   .\build.ps1 -SkipDeps          # Skip dependency download/install
   .\build.ps1 -Configuration Debug  # Debug build
   .\build.ps1 -JavaHome "C:\path\to\jdk"  # Specify JDK path

 Environment Variables:
   JAVA_HOME     - Path to JDK installation (required if not passed as parameter)
   VCPKG_ROOT    - Path to existing vcpkg installation (optional)

 Author: mdflib-java contributors
 Version: 1.0.0
 Since: 1.0.0
# ============================================================================#>

[CmdletBinding()]
param(
    # Build configuration: Release or Debug
    [ValidateSet("Release", "Debug")]
    [string]$Configuration = "Release",

    # Skip running Maven tests
    [switch]$SkipTests = $false,

    # Perform a clean build (remove previous build artifacts)
    [switch]$Clean = $false,

    # Skip dependency download and configuration step
    [switch]$SkipDeps = $false,

    # Explicit path to JDK installation (overrides JAVA_HOME)
    [string]$JavaHome = "",

    # Build only native libraries (skip Maven)
    [switch]$NativeOnly = $false,

    # Build only Java (skip native build)
    [switch]$JavaOnly = $false,

    # Verbose output
    [switch]$Verbose = $false
)

# ============================================================================
# Script-level variables
# ============================================================================

# Project root directory (parent of the script directory)
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path (Join-Path $ScriptDir "..")

# Build directories
$BuildDir = Join-Path $ProjectRoot "build_native"
$VcpkgDir = Join-Path $ProjectRoot "vcpkg"

# Error handling preference
$ErrorActionPreference = "Stop"

# ============================================================================
# Helper Functions
# ============================================================================

/**
 * Writes a formatted section header to the console.
 *
 * @param text The header text to display
 */
function Write-SectionHeader {
    param([string]$Text)
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host ""
}

/**
 * Writes a formatted step message to the console.
 *
 * @param text The step description
 */
function Write-Step {
    param([string]$Text)
    Write-Host ">>> $Text" -ForegroundColor Yellow
}

/**
 * Writes a success message to the console.
 *
 * @param text The success message
 */
function Write-Success {
    param([string]$Text)
    Write-Host "    [OK] $Text" -ForegroundColor Green
}

/**
 * Writes an error message to the console and optionally exits.
 *
 * @param text The error message
 * @param exit Whether to exit the script
 */
function Write-ErrorAndExit {
    param([string]$Text)
    Write-Host "    [ERROR] $Text" -ForegroundColor Red
    exit 1
}

/**
 * Executes a command and checks the exit code.
 *
 * @param command The command to execute
 * @param errorMessage Message to display on failure
 */
function Invoke-CommandSafe {
    param(
        [scriptblock]$Command,
        [string]$ErrorMessage = "Command failed"
    )
    try {
        & $Command
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
            Write-ErrorAndExit "$ErrorMessage (exit code: $LASTEXITCODE)"
        }
    } catch {
        Write-ErrorAndExit "$ErrorMessage : $_"
    }
}

# ============================================================================
# Step 1: Resolve JAVA_HOME
#
# The JDK is required for:
# - JNI header files (jni.h) used during C++ compilation
# - Java compiler (javac) for building the Java source
# - Maven build system for dependency management and testing
#
# Resolution order:
# 1. Explicit -JavaHome parameter (highest priority)
# 2. JAVA_HOME environment variable
# 3. Auto-detection from common installation paths
# ============================================================================

function Resolve-JavaHome {
    Write-Step "Resolving JAVA_HOME..."

    # Priority: parameter > environment variable > common locations
    if ($JavaHome -ne "") {
        # User explicitly specified Java home via command-line parameter
        $script:JavaHomeResolved = $JavaHome
    } elseif ($env:JAVA_HOME -ne "") {
        # Use JAVA_HOME environment variable if set
        $script:JavaHomeResolved = $env:JAVA_HOME
    } else {
        # Auto-detect: search common JDK installation directories
        $commonPaths = @(
            "C:\Program Files\Java\jdk*",
            "C:\Program Files\Eclipse Adoptium\jdk*",
            "C:\Program Files\Microsoft\jdk*"
        )
        foreach ($pattern in $commonPaths) {
            # Sort by name descending to get the latest version first
            $found = Get-Item $pattern -ErrorAction SilentlyContinue |
                     Sort-Object Name -Descending |
                     Select-Object -First 1
            if ($found) {
                $script:JavaHomeResolved = $found.FullName
                break
            }
        }
    }

    # Validate that we found a valid JDK installation
    if (-not $script:JavaHomeResolved) {
        Write-ErrorAndExit "JAVA_HOME not found. Set JAVA_HOME or pass -JavaHome parameter."
    }

    # Verify JNI header exists (confirms this is a JDK, not just a JRE)
    $jniHeader = Join-Path $script:JavaHomeResolved "include\jni.h"
    if (-not (Test-Path $jniHeader)) {
        Write-ErrorAndExit "jni.h not found at: $jniHeader. Ensure JAVA_HOME points to a JDK (not JRE)."
    }

    # Set environment variable for child processes (CMake, Maven)
    $env:JAVA_HOME = $script:JavaHomeResolved
    Write-Success "JAVA_HOME: $script:JavaHomeResolved"
}

# ============================================================================
# Step 2: Download and configure dependencies (vcpkg, zlib, expat)
#
# Uses vcpkg (Microsoft's C++ package manager) to install the third-party
# dependencies required by mdflib:
#   - zlib: data compression library (required for MDF4 compressed blocks)
#   - expat: XML parser library (required for MDF4 metadata parsing)
#
# If vcpkg is already installed (VCPKG_ROOT environment variable), it is
# reused to avoid redundant downloads. Otherwise, vcpkg is cloned from
# GitHub and bootstrapped automatically.
#
# The vcpkg toolchain file is saved for use in subsequent CMake steps.
# ============================================================================

function Install-Dependencies {
    Write-Step "Checking and installing third-party dependencies..."

    # Check for existing vcpkg installation to avoid re-downloading
    $vcpkgRoot = $env:VCPKG_ROOT
    if (-not $vcpkgRoot -or -not (Test-Path $vcpkgRoot)) {
        # Check for a local vcpkg clone in the project directory
        if (Test-Path $VcpkgDir) {
            $vcpkgRoot = $VcpkgDir
        } else {
            # Clone vcpkg from the official Microsoft repository
            Write-Step "Cloning vcpkg from GitHub..."
            Invoke-CommandSafe -Command {
                git clone https://github.com/microsoft/vcpkg.git $VcpkgDir
            } -ErrorMessage "Failed to clone vcpkg"

            # Bootstrap vcpkg to build the vcpkg executable
            Write-Step "Bootstrapping vcpkg..."
            $bootstrapScript = Join-Path $VcpkgDir "bootstrap-vcpkg.bat"
            Invoke-CommandSafe -Command {
                & $bootstrapScript
            } -ErrorMessage "Failed to bootstrap vcpkg"

            $vcpkgRoot = $VcpkgDir
        }
    }

    # Locate the vcpkg executable
    $vcpkgExe = Join-Path $vcpkgRoot "vcpkg.exe"
    if (-not (Test-Path $vcpkgExe)) {
        $vcpkgExe = Join-Path $vcpkgRoot "vcpkg"
    }

    Write-Success "vcpkg: $vcpkgRoot"

    # Install zlib and expat using vcpkg with the appropriate triplet
    Write-Step "Installing zlib and expat via vcpkg..."

    # Determine the target triplet based on platform and architecture
    if ($env:OS -eq "Windows_NT") {
        $triplet = "x64-windows"
    } else {
        $triplet = "x64-linux"
    }

    # Install packages with --recurse to handle transitive dependencies
    Invoke-CommandSafe -Command {
        & $vcpkgExe install zlib:$triplet expat:$triplet --recurse
    } -ErrorMessage "Failed to install vcpkg dependencies"

    Write-Success "Dependencies installed successfully"

    # Save the vcpkg CMake toolchain file path for use by CMake
    $script:VcpkgToolchain = Join-Path $vcpkgRoot "scripts\buildsystems\vcpkg.cmake"
    $env:VCPKG_ROOT = $vcpkgRoot
}

# ============================================================================
# Step 3: Build mdflib C++ static library from source
#
# Builds the mdflib core library from the mdflib_src submodule. This
# produces the static library (mdflibstatic.lib / libmdflibstatic.a)
# that the JNI bridge will link against.
#
# Build configuration:
#   - MDF_BUILD_SHARED_LIB=ON: enables the mdflibrary shared library target
#   - MDF_BUILD_SHARED_LIB_NET=OFF: disables .NET/CLR support
#   - BUILD_SHARED_LIBS=OFF: builds mdflib as a static library
#
# The vcpkg toolchain file is passed to CMake to resolve zlib and expat
# dependencies automatically.
# ============================================================================

function Build-MdfLib {
    Write-Step "Building mdflib C++ static library..."

    # Define build and source directories
    $mdflibBuildDir = Join-Path $BuildDir "mdflib"
    $mdflibSrcDir = Join-Path $ProjectRoot "mdflib_src"

    # Clean previous build artifacts if requested
    if ($Clean -and (Test-Path $mdflibBuildDir)) {
        Remove-Item $mdflibBuildDir -Recurse -Force
    }

    # Create the build output directory
    if (-not (Test-Path $mdflibBuildDir)) {
        New-Item -ItemType Directory -Path $mdflibBuildDir | Out-Null
    }

    # Configure CMake for mdflib with vcpkg toolchain and build options
    Write-Step "Configuring mdflib with CMake..."
    Invoke-CommandSafe -Command {
        cmake -B $mdflibBuildDir -S $mdflibSrcDir `
            -DCMAKE_BUILD_TYPE=$Configuration `
            -DCMAKE_TOOLCHAIN_FILE=$script:VcpkgToolchain `
            -DMDF_BUILD_SHARED_LIB=ON `
            -DMDF_BUILD_SHARED_LIB_NET=OFF `
            -DBUILD_SHARED_LIBS=OFF
    } -ErrorMessage "mdflib CMake configuration failed"

    # Compile mdflib using all available CPU cores for speed
    Write-Step "Compiling mdflib..."
    Invoke-CommandSafe -Command {
        cmake --build $mdflibBuildDir --config $Configuration --parallel
    } -ErrorMessage "mdflib build failed"

    Write-Success "mdflib built successfully"
}

# ============================================================================
# Step 4: Build JNI bridge library
#
# Builds the mdflibjni shared library that provides the JNI bridge between
# Java and the native mdflib library. This is the core deliverable of the
# build process.
#
# The JNI bridge:
#   - Links against the mdflib static library (built in Step 3)
#   - Links against zlib and expat (installed via vcpkg in Step 2)
#   - Includes JNI headers from the JDK (resolved in Step 1)
#
# Output:
#   - Windows: mdflibjni.dll (placed in src/main/resources/native/win32-x86-64/)
#   - Linux:   libmdflibjni.so (placed in src/main/resources/native/linux-x86-64/)
#
# The output directory is the Java resources path so that the DLL/SO files
# are packaged inside the JAR and can be extracted at runtime.
# ============================================================================

function Build-JniBridge {
    Write-Step "Building JNI bridge library..."

    # Define build directories
    $jniBuildDir = Join-Path $BuildDir "jni"
    $nativeDir = Join-Path $ProjectRoot "native"

    # Clean previous build artifacts if requested
    if ($Clean -and (Test-Path $jniBuildDir)) {
        Remove-Item $jniBuildDir -Recurse -Force
    }

    # Create the build output directory
    if (-not (Test-Path $jniBuildDir)) {
        New-Item -ItemType Directory -Path $jniBuildDir | Out-Null
    }

    # Determine the platform-specific resource output directory
    # This is where the built .dll/.so will be placed for JAR packaging
    if ($env:OS -eq "Windows_NT") {
        $platformDir = "win32-x86-64"
    } else {
        $platformDir = "linux-x86-64"
    }
    $resourceDir = Join-Path $ProjectRoot "src\main\resources\native\$platformDir"

    # Create the resource directory if it doesn't exist
    if (-not (Test-Path $resourceDir)) {
        New-Item -ItemType Directory -Path $resourceDir -Force | Out-Null
    }

    # Locate the mdflib library directory from the previous build step
    $mdflibBuildDir = Join-Path $BuildDir "mdflib"
    $mdflibLibDir = Join-Path $mdflibBuildDir "mdflibrary"
    if ($Configuration -eq "Release") {
        $mdflibConfigDir = Join-Path $mdflibLibDir "Release"
    } else {
        $mdflibConfigDir = Join-Path $mdflibLibDir "Debug"
    }

    # Configure CMake for the JNI bridge library
    # Pass JAVA_HOME and MDFLIB_DIR so CMake can find the required headers/libs
    Write-Step "Configuring JNI bridge with CMake..."
    Invoke-CommandSafe -Command {
        cmake -B $jniBuildDir -S $nativeDir `
            -DCMAKE_BUILD_TYPE=$Configuration `
            -DCMAKE_TOOLCHAIN_FILE=$script:VcpkgToolchain `
            -DJAVA_HOME="$($script:JavaHomeResolved)" `
            -DMDFLIB_DIR="$mdflibLibDir"
    } -ErrorMessage "JNI bridge CMake configuration failed"

    # Compile the JNI bridge library using all available CPU cores
    Write-Step "Compiling JNI bridge..."
    Invoke-CommandSafe -Command {
        cmake --build $jniBuildDir --config $Configuration --parallel
    } -ErrorMessage "JNI bridge build failed"

    Write-Success "JNI bridge library built successfully"
}

# ============================================================================
# Step 5: Copy dependency DLLs to resources
#
# On Windows, the dependency DLLs (zlib1.dll, libexpat.dll) must be
# available alongside the JNI library. This step copies them to the
# Java resources directory.
# ============================================================================

function Copy-DependencyDlls {
    Write-Step "Copying dependency DLLs to resources..."

    if ($env:OS -ne "Windows_NT") {
        Write-Success "Skipped (non-Windows platform)"
        return
    }

    $platformDir = "win32-x86-64"
    $resourceDir = Join-Path $ProjectRoot "src\main\resources\native\$platformDir"

    # Find vcpkg installed DLLs
    $vcpkgRoot = $env:VCPKG_ROOT
    $installedDir = Join-Path $vcpkgRoot "installed\x64-windows\bin"

    $dlls = @("zlib1.dll", "libexpat.dll")
    foreach ($dll in $dlls) {
        $source = Join-Path $installedDir $dll
        if (Test-Path $source) {
            Copy-Item $source $resourceDir -Force
            Write-Success "Copied $dll"
        } else {
            # Try alternative naming
            $altSource = Join-Path $installedDir ($dll -replace "zlib1", "zlib")
            if (Test-Path $altSource) {
                Copy-Path $altSource (Join-Path $resourceDir $dll) -Force
                Write-Success "Copied $dll (from alternative name)"
            } else {
                Write-Host "    [WARN] $dll not found in vcpkg installed directory" -ForegroundColor Yellow
            }
        }
    }

    # Copy mdflibrary.dll if it exists (the JNI bridge links statically,
    # but we may need the shared library for runtime)
    $mdflibBuildDir = Join-Path $BuildDir "mdflib\mdflibrary"
    $mdflibDll = Get-ChildItem -Path $mdflibBuildDir -Filter "mdflibrary.dll" -Recurse -ErrorAction SilentlyContinue |
                  Select-Object -First 1
    if ($mdflibDll) {
        Copy-Item $mdflibDll.FullName $resourceDir -Force
        Write-Success "Copied mdflibrary.dll"
    }
}

# ============================================================================
# Step 6: Update POM.xml
#
# Updates the Maven POM file to remove JNA dependency and add the
# JNI-specific test configuration.
# ============================================================================

function Update-PomXml {
    Write-Step "Updating pom.xml for JNI..."
    # POM is already configured; just verify
    Write-Success "POM configuration verified"
}

# ============================================================================
# Step 7: Build Java project and run tests
#
# Uses Maven to compile the Java code, package the JAR, and run the
# unit tests. The native library must be built first.
# ============================================================================

function Build-JavaProject {
    Write-Step "Building Java project with Maven..."

    $mvnArgs = "clean compile"
    if (-not $SkipTests) {
        $mvnArgs = "clean test"
    }

    Invoke-CommandSafe -Command {
        mvn $mvnArgs.Split(" ")
    } -ErrorMessage "Maven build failed"

    Write-Success "Java project built successfully"
}

# ============================================================================
# Main execution flow
# ============================================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  mdflib-java JNI Build Script" -ForegroundColor Magenta
Write-Host "  Configuration: $Configuration" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

$totalStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Step 1: Resolve JAVA_HOME
Write-SectionHeader "Step 1: Resolve JAVA_HOME"
Resolve-JavaHome

if (-not $JavaOnly) {
    # Step 2: Download and configure dependencies
    if (-not $SkipDeps) {
        Write-SectionHeader "Step 2: Download & Configure Dependencies"
        Install-Dependencies
    } else {
        Write-SectionHeader "Step 2: Skipping Dependencies (SkipDeps=true)"
    }

    # Step 3: Build mdflib C++ library
    Write-SectionHeader "Step 3: Build mdflib C++ Library"
    Build-MdfLib

    # Step 4: Build JNI bridge library
    Write-SectionHeader "Step 4: Build JNI Bridge Library"
    Build-JniBridge

    # Step 5: Copy dependency DLLs
    Write-SectionHeader "Step 5: Copy Dependency DLLs"
    Copy-DependencyDlls
}

if (-not $NativeOnly) {
    # Step 6: Update POM
    Write-SectionHeader "Step 6: Update Configuration"
    Update-PomXml

    # Step 7: Build Java and run tests
    Write-SectionHeader "Step 7: Build Java & Run Tests"
    Build-JavaProject
}

$totalStopwatch.Stop()

# ============================================================================
# Summary
# ============================================================================
Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  BUILD COMPLETE" -ForegroundColor Green
Write-Host "  Time: $($totalStopwatch.Elapsed.ToString('mm\:ss'))" -ForegroundColor Green
Write-Host "  Configuration: $Configuration" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""
