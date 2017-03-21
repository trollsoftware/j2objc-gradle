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

import com.github.j2objccontrib.j2objcgradle.tasks.AssembleResourcesTask
import com.github.j2objccontrib.j2objcgradle.tasks.AssembleSourceTask
import com.github.j2objccontrib.j2objcgradle.tasks.CycleFinderTask

import com.github.j2objccontrib.j2objcgradle.tasks.TranslateTask
import com.github.j2objccontrib.j2objcgradle.tasks.Utils

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.logging.LogLevel

/*
 * Main plugin class for creation of extension object and all the tasks.
 */
class J2objcPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        String version = BuildInfo.VERSION
        String commit = BuildInfo.GIT_COMMIT
        String url = BuildInfo.URL
        String timestamp = BuildInfo.TIMESTAMP
        project.logger.info("j2objc-gradle plugin: Version $version, Built: $timestamp, Commit: $commit, URL: $url")
        if (!BuildInfo.GIT_IS_CLEAN) {
            project.logger.error('WARNING: j2objc-gradle plugin was built with local git modification: ' +
                                 'https://github.com/j2objc-contrib/j2objc-gradle/releases')
        } else if (version.contains('SNAPSHOT')) {
            project.logger.warn('WARNING: j2objc-gradle plugin was built with SNAPSHOT version: ' +
                                'https://github.com/j2objc-contrib/j2objc-gradle/releases')
        }

        // This avoids a lot of "project." prefixes, such as "project.tasks.create"
        project.with {
            getPluginManager().apply(JavaPlugin)

            extensions.create('j2objcConfig', J2objcConfig, project)

            afterEvaluate { Project evaluatedProject ->
                Utils.throwIfNoJavaPlugin(evaluatedProject)

                if (!evaluatedProject.j2objcConfig.isFinalConfigured()) {
                    logger.error("Project '${evaluatedProject.name}' is missing finalConfigure():\n" +
                                 "https://github.com/j2objc-contrib/j2objc-gradle/blob/master/FAQ.md#how-do-i-call-finalconfigure")
                }
            }

            // This is an intermediate directory only.  Clients should use only directories
            // specified in j2objcConfig (or associated defaults in J2objcConfig).
            File j2objcSrcGenMainDir = file("${buildDir}/j2objcSrcGenMain")
            File j2objcSrcGenTestDir = file("${buildDir}/j2objcSrcGenTest")

            // These configurations are groups of artifacts and dependencies for the plugin build
            // https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
            configurations {
                // When j2objcConfig.autoConfigureDeps is true, this configuration
                // will have source paths automatically added to it.  You can add
                // *source* JARs/directories yourself as well.
                j2objcTranslationClosure {
                    description = 'J2ObjC Java source dependencies that need to be ' +
                                  'partially translated via --build-closure'
                }
                // There is no corresponding j2objcTestTranslationClosure - building test code with
                // --build-closure will almost certainly cause duplicate symbols when linked with
                // main code.

                // If you want to translate an entire source library, regardless of which parts of the
                // library code your own code depends on, use this configuration.  This
                // will also cause the library to be Java compiled, since you cannot translate
                // a library with j2objc that does not successfully compile in Java.
                // So for example:
                //   dependencies { j2objcTranslation 'com.google.code.gson:gson:2.3.1:sources' }
                // will cause your project to produce a full gson Java classfile Jar AND a
                // j2objc-translated static native library.
                j2objcTranslation {
                    description = 'J2ObjC Java source libraries that should be fully translated ' +
                                  'and built as stand-alone native libraries'
                }
                // Currently, we can only handle Project dependencies already translated to Objective-C.
                j2objcLinkage {
                    description = 'J2ObjC native library dependencies that need to be ' +
                                  'linked into the final main library, and do not need translation'
                }
            }

            DependencyResolver.configureSourceSets(project)

            // Produces a modest amount of output
            logging.captureStandardOutput LogLevel.INFO

            // If users need to generate extra files that j2objc depends on, they can make this task dependent
            // on such generation.
            tasks.create(name: 'j2objcPreBuild', type: DefaultTask,
                    dependsOn: 'test') {
                group 'build'
                description "Marker task for all tasks that must be complete before j2objc building"
            }

            // TODO @Bruno "build/source/apt" must be project.j2objcConfig.generatedSourceDirs no idea how to set it
            // there
            // Dependency may be added in project.plugins.withType for Java or Android plugin
            tasks.create(name: 'j2objcTranslate', type: TranslateTask,
                    dependsOn: 'j2objcPreBuild') {
                group 'build'
                description "Translates all the java source files in to Objective-C using 'j2objc'"
                // Output directories of 'j2objcTranslate', input for all other tasks
                srcGenMainDir = j2objcSrcGenMainDir
                srcGenTestDir = j2objcSrcGenTestDir
            }

            // j2objcCycleFinder is disabled by default as it's complex to use and understand.
            // TODO: consider enabling by default if it's possible to make it easier to use.
            // To enable the j2objcCycleFinder task, add the following to build.gradle:
            // j2objcCycleFinder { enabled = true }
            tasks.create(name: 'j2objcCycleFinder', type: CycleFinderTask,
                    dependsOn: 'j2objcPreBuild') {
                group 'build'
                description "Run the cycle_finder tool on all Java source files"
                enabled false
            }
            // Assemble files
            tasks.create(name: 'j2objcAssembleResources', type: AssembleResourcesTask,
                    dependsOn: ['j2objcPreBuild']) {
                group 'build'
                description 'Copies mains and test resources to assembly directories'
            }
            tasks.create(name: 'j2objcAssembleSource', type: AssembleSourceTask,
                    dependsOn: ['j2objcTranslate']) {
                group 'build'
                description 'Copies final generated source to assembly directories'
                srcGenMainDir = j2objcSrcGenMainDir
                srcGenTestDir = j2objcSrcGenTestDir
            }
        }
    }
}
