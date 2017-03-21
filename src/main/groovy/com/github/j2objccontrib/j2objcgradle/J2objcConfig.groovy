/*
 * Copyright (c) 2015 the authors of j2objc-gradle (see AUTHORS file)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.j2objccontrib.j2objcgradle

import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import com.google.common.annotations.VisibleForTesting
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil

/**
 * j2objcConfig is used to configure the plugin with the project's build.gradle.
 *
 * All paths are resolved using Gradle's <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#file(java.lang.Object)">project.file(...)</a>
 *
 *
 * Basic Example:
 *
 * j2objcConfig {
 *     xcodeProjectDir '../ios'
 *     xcodeTarget 'IOS-APP'
 *     finalConfigure()
 * }
 *
 *
 * Complex Example:
 *
 * TODO...
 *
 */
@CompileStatic
class J2objcConfig {

    static J2objcConfig from(Project project) {
        return project.extensions.findByType(J2objcConfig)
    }

    final protected Project project

    J2objcConfig(Project project) {
        assert project != null
        this.project = project

        destSrcMainDir = new File(project.buildDir, 'j2objcOutputs/src/main').absolutePath
        destSrcTestDir = new File(project.buildDir, 'j2objcOutputs/src/test').absolutePath
    }

    /**
     * Where to assemble generated main source and resources files.
     * <p/>
     * Defaults to $buildDir/j2objcOutputs/src/main/objc
     */
    String destSrcMainDir = null

    /**
     * Where to assemble generated test source and resources files.
     * <p/>
     * Can be the same directory as destDir
     * Defaults to $buildDir/j2objcOutputs/src/test/objc
     */
    String destSrcTestDir = null

    File getDestSrcDirFile(String sourceSetName, String fileType) {
        assert sourceSetName in ['main', 'test']
        assert fileType in ['objc', 'resources']

        File destSrcDir = null
        if (sourceSetName == 'main') {
            destSrcDir = project.file(destSrcMainDir)
        } else if (sourceSetName == 'test') {
            destSrcDir = project.file(destSrcTestDir)
        } else {
            assert false, "Unsupported sourceSetName: $sourceSetName"
        }

        return project.file(new File(destSrcDir, fileType))
    }

    /**
     * Generated source files directories, e.g. from dagger annotations.
     */
    // Default location for generated source files using annotation processor compilation,
    // per sourceSets.main.output.classesDir.
    // However, we cannot actually access sourceSets.main.output.classesDir here, because
    // the Java plugin convention may not be applied at this time.
    // TODO: Add a test counterpart for this.
    List<String> generatedSourceDirs = ['build/classes/main']
    /**
     * Add generated source files directories, e.g. from dagger annotations.
     *
     * @param generatedSourceDirs adds generated source directories for j2objc translate
     */
    void generatedSourceDirs(String... generatedSourceDirs) {
        appendArgs(this.generatedSourceDirs, 'generatedSourceDirs', true, generatedSourceDirs)
    }


    // CYCLEFINDER
    /**
     * Command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     */
    List<String> cycleFinderArgs = new ArrayList<>()
    /**
     * Add command line arguments for j2objc cycle_finder.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/cycle_finder.html
     *
     * @param cycleFinderArgs add args for 'cycle_finder' tool
     */
    void cycleFinderArgs(String... cycleFinderArgs) {
        appendArgs(this.cycleFinderArgs, 'cycleFinderArgs', true, cycleFinderArgs)
    }
    /**
     * Expected number of cycles, defaults to all those found in JRE.
     * <p/>
     * This is an exact number rather than minimum as any change is significant.
     */
    // TODO(bruno): convert to a default whitelist and change expected cyles to 0
    int cycleFinderExpectedCycles = 40


    // TRANSLATE
    /**
     * Command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     */
    List<String> translateArgs = new ArrayList<>()
    /**
     * Add command line arguments for j2objc translate.
     * <p/>
     * A list of all possible arguments can be found here:
     * http://j2objc.org/docs/j2objc.html
     *
     * @param translateArgs add args for the 'j2objc' tool
     */
    void translateArgs(String... translateArgs) {
        appendArgs(this.translateArgs, 'translateArgs', true, translateArgs)
    }

    /**
     * Enables --build-closure, which translates classes referenced from the
     * list of files passed for translation, using the
     * {@link #translateSourcepaths}.
     */
    void enableBuildClosure() {
        if (!translateArgs.contains('--build-closure')) {
            translateArgs('--build-closure')
        }
    }

    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     */
    List<String> translateClasspaths = new ArrayList<>()
    /**
     *  Local jars for translation e.g.: "lib/json-20140107.jar", "lib/somelib.jar".
     *  This will be added to j2objc as a '-classpath' argument.
     *
     *  @param translateClasspaths add libraries for -classpath argument
     */
    void translateClasspaths(String... translateClasspaths) {
        appendArgs(this.translateClasspaths, 'translateClasspaths', true, translateClasspaths)
    }

    /**
     * Source jars for translation e.g.: "lib/json-20140107-sources.jar"
     */
    List<String> translateSourcepaths = new ArrayList<>()
    /**
     * Source jars for translation e.g.: "lib/json-20140107-sources.jar"
     *
     *  @param translateSourcepaths args add source jar for translation
     */
    void translateSourcepaths(String... translateSourcepaths) {
        appendArgs(this.translateSourcepaths, 'translateSourcepaths', true, translateSourcepaths)
    }

    /**
     * True iff only translation (and cycle finding, if applicable) should be attempted,
     * skipping all compilation, linking, and testing tasks.
     */
    boolean translateOnlyMode = false


    // Do not use groovydoc, this option should remain undocumented.
    // WARNING: Do not use this unless you know what you are doing.
    // If true, incremental builds will be supported even if --build-closure is included in
    // translateArgs. This may break the build in unexpected ways if you change the dependencies
    // (e.g. adding new files or changing translateClasspaths). When you change the dependencies and
    // the build breaks, you need to do a clean build.
    boolean UNSAFE_incrementalBuildClosure = false

    /**
     * Experimental functionality to automatically configure dependencies.
     * Consider you have dependencies like:
     * <pre>
     * dependencies {
     *     compile project(':peer1')                  // type (1)
     *     compile 'com.google.code.gson:gson:2.3.1'  // type (3)
     *     compile 'com.google.guava:guava:18.0'      // type (2)
     *     testCompile 'junit:junit:4.11'             // type (2)
     * }
     * </pre>
     * Project dependencies (1) will be added as a `j2objcLink` dependency.
     * Libraries already included in j2objc (2) will be ignored.
     * External libraries in Maven (3) will be added in source JAR form to
     * `j2objcTranslate`, and translated using `--build-closure`.
     * Dependencies must be fully specified before you call finalConfigure().
     * <p/>
     * This will become the default when stable in future releases.
     */
    boolean autoConfigureDeps = false

    /**
     * Additional Java libraries that are part of the j2objc distribution.
     * <p/>
     * For example:
     * <pre>
     * translateJ2objcLibs = ["j2objc_junit.jar", "jre_emul.jar"]
     * </pre>
     */
    // J2objc default libraries, from $J2OBJC_HOME/lib/...
    // TODO: auto add libraries based on java dependencies, warn on version differences
    List<String> translateJ2objcLibs = [
            // Comments indicate difference compared to standard libraries...
            // Memory annotations, e.g. @Weak, @AutoreleasePool
            "j2objc_annotations.jar",
            // Libraries that have CycleFinder fixes, e.g. @Weak and code removal
            "j2objc_guava.jar", "j2objc_junit.jar", "jre_emul.jar",
            // Libraries that don't need CycleFinder fixes
            "javax.inject-1.jar", "jsr305-3.0.0.jar",
            "mockito-core-1.9.5.jar", "hamcrest-core-1.3.jar", "protobuf_runtime.jar"]

    /**
     * Additional native libraries that are part of the j2objc distribution to link
     * with the production code (and also the test code).
     * <p/>
     * For example:
     * <pre>
     * linkJ2objcLibs = ["guava", "jsr305"]
     * </pre>
     */
    // J2objc default libraries, from $J2OBJC_HOME/lib/..., without '.a' extension.
    // TODO: auto add libraries based on java dependencies, warn on version differences
    List<String> linkJ2objcLibs = ['guava', 'javax_inject', 'jsr305', 'protobuf_runtime']

    /**
     * Additional native libraries that are part of the j2objc distribution to link
     * with the test code.
     */
    // J2objc default libraries, from $J2OBJC_HOME/lib/..., without '.a' extension.
    // TODO: auto add libraries based on java dependencies, warn on version differences
    // Note: Hamcrest appears to be included within libjunit.a.
    List<String> linkJ2objcTestLibs = ['junit', 'mockito', 'j2objc_main']

    // TODO: warn if different versions than testCompile from Java plugin
    /**
     * Force filename collision check so prohibit two files with same name.
     * <p/>
     * This will automatically be set to true when translateArgs contains
     * '--no-package-directories'. That flag flattens the directory structure
     * and will overwrite files with the same name.
     */
    boolean forceFilenameCollisionCheck = false

    // All access to filenameCollisionCheck should be done through this function
    boolean getFilenameCollisionCheck() {
        if (translateArgs.contains('--no-package-directories'))
            return true
        return forceFilenameCollisionCheck
    }

    /**
     * Sets the filter on files to translate.
     * <p/>
     * If no pattern is specified, all files within the sourceSets are translated.
     * <p/>
     * This filter is applied on top of all files within the 'main' and 'test'
     * java sourceSets.  Use {@link #translatePattern(groovy.lang.Closure)} to
     * configure.
     */
    PatternSet translatePattern = null
    /**
     * Configures the {@link #translatePattern}.
     * <p/>
     * Calling this method repeatedly further modifies the existing translatePattern,
     * and will create an empty translatePattern if none exists.
     * <p/>
     * For example:
     * <pre>
     * translatePattern {
     *     exclude 'CannotTranslateFile.java'
     *     exclude '**&#47;CannotTranslateDir&#47;*.java'
     *     include '**&#47;CannotTranslateDir&#47;AnExceptionToInclude.java'
     * }
     * </pre>
     * @see
     * <a href="https://docs.gradle.org/current/userguide/working_with_files.html#sec:file_trees">Gradle User Guide</a>
     */
    PatternSet translatePattern(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PatternSet) Closure cl) {
        if (translatePattern == null) {
            translatePattern = new PatternSet()
        }
        return ConfigureUtil.configure(cl, translatePattern)
    }

    /**
     * A mapping from source file names (in the project Java sourcesets) to alternate
     * source files.
     * Both before and after names (keys and values) are evaluated using project.file(...).
     * <p/>
     * Mappings can be used to have completely different implementations in your Java
     * jar vs. your Objective-C library.  This can be especially useful when compiling
     * a third-party library and you need to provide non-trivial OCNI implementations
     * in Objective-C.
     */
    Map<String, String> translateSourceMapping = [:]

    /**
     * Adds a new source mapping.
     * @see #translateSourceMapping
     */
    void translateSourceMapping(String before, String after) {
        translateSourceMapping.put(before, after)
    }

    protected boolean finalConfigured = false
    /**
     * Configures the j2objc build.  Must be called at the very
     * end of your j2objcConfig block.
     */
    @VisibleForTesting
    void finalConfigure() {
        validateConfiguration()
        // Conversion of compile and testCompile dependencies occurs optionally.
        if (autoConfigureDeps) {
            convertDeps()
        }
        // Resolution of j2objcTranslateSource dependencies occurs always.
        // This lets users turn off autoConfigureDeps but manually set j2objcTranslateSource.
        resolveDeps()
        configureTaskState()
        finalConfigured = true
    }

    protected void verifyJ2objcRequirements() {
        // Verify that underlying J2ObjC binary exists at all.
        File j2objcJar = Utils.j2objcJar(project)
        if (!j2objcJar.exists()) {
            Utils.throwJ2objcConfigFailure(project, "J2ObjC binary does not exist at ${j2objcJar.absolutePath}.")
        }
    }

    protected void validateConfiguration() {
        // Validate minimally required parameters.
        verifyJ2objcRequirements()

        assert destSrcMainDir != null
        assert destSrcTestDir != null
    }

    protected void convertDeps() {
        new DependencyConverter(project, this).configureAll()
    }

    protected void resolveDeps() {
        new DependencyResolver(project, this).configureAll()
    }

    protected void configureTaskState() {
        // Disable only if explicitly present and not true.
        boolean debugEnabled = Boolean.parseBoolean(Utils.getLocalProperty(project, 'debug.enabled', 'true'))
        boolean releaseEnabled = Boolean.parseBoolean(Utils.getLocalProperty(project, 'release.enabled', 'true'))
        // Enable only if explicitly present in either the project config OR the local config.
        boolean translateOnlyMode = this.translateOnlyMode ||
                                    Boolean.parseBoolean(Utils.getLocalProperty(project, 'translateOnlyMode', 'false'))

        if (!translateOnlyMode) {
            Utils.requireMacOSX('Native Compilation of translated code task')
        }

        project.logger.info("J2objcPlugin: translateOnlyMode will disable most j2objc tasks")

        project.tasks.all { Task task ->
            String name = task.name
            // For convenience, disable all debug and/or release tasks if the user desires.
            // Note all J2objcPlugin-created tasks are of the form `j2objc.*(Debug|Release)?`
            // however Native plugin-created tasks (on our behalf) are of the form `.*((D|d)ebug|(R|r)elease).*(j|J)2objc.*'
            // so these patterns find all such tasks.
            if (name.contains('j2objc') || name.contains('J2objc')) {
                if (!debugEnabled && (name.contains('debug') || name.contains('Debug'))) {
                    task.enabled = false
                }
                if (!releaseEnabled && (name.contains('release') || name.contains('Release'))) {
                    task.enabled = false
                }
            }

            // Support translation-only mode.
            if (translateOnlyMode) {
                // First pattern matches all native-compilation tasks.
                // Second pattern matches plugin-specific tasks beyond translation.
                if ((name =~ /^.*((J|j)2objc(Executable|StaticLibrary|SharedLibrary|Objc))$/).matches() ||
                    (name =~ /^j2objc(Assemble|PackLibraries|Test)(Debug|Release)$/).matches()) {
                    task.enabled = false
                }
            }
        }
    }

    boolean isFinalConfigured() {
        return finalConfigured
    }

    // Provides a subset of "args" interface from project.exec as implemented by ExecHandleBuilder:
    // https://github.com/gradle/gradle/blob/master/subprojects/core/src/main/groovy/org/gradle/process/internal/ExecHandleBuilder.java
    // Allows the following:
    // j2objcConfig {
    //     translateArgs '--no-package-directories', '--prefixes', 'prefixes.properties'
    // }
    @VisibleForTesting
    static void appendArgs(List<String> listArgs, String nameArgs, boolean rejectSpaces, String... args) {
        verifyArgs(nameArgs, rejectSpaces, args)
        listArgs.addAll(Arrays.asList(args))
    }

    // Verify that no argument contains a space
    @VisibleForTesting
    static void verifyArgs(String nameArgs, boolean rejectSpaces, String... args) {
        if (args == null) {
            throw new InvalidUserDataException("$nameArgs == null!")
        }
        for (String arg in args) {
            if (arg.isAllWhitespace()) {
                throw new InvalidUserDataException(
                        "$nameArgs is all whitespace: '$arg'")
            }
            if (rejectSpaces) {
                if (arg.contains(' ')) {
                    String rewrittenArgs = "'" + arg.split(' ').join("', '") + "'"
                    throw new InvalidUserDataException(
                            "'$arg' argument should not contain spaces and be written out as distinct entries:\n" +
                            "$nameArgs $rewrittenArgs")
                }
            }
        }
    }

    @VisibleForTesting
    void testingOnlyPrepConfigurations() {
        // When testing we don't always want to apply the entire plugin
        // before calling finalConfigure.
        project.configurations.create('j2objcTranslationClosure')
        project.configurations.create('j2objcTranslation')
    }
}
