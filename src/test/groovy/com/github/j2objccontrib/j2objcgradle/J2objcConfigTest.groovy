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

import com.github.j2objccontrib.j2objcgradle.tasks.TestingUtils
import com.github.j2objccontrib.j2objcgradle.tasks.Utils
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.ConfigureUtil
import org.hamcrest.core.StringContains
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.testng.Assert

/**
 * J2objcConfig tests.
 */
@CompileStatic
class J2objcConfigTest {

    private Project proj

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    void setUp() {
        // Default to native OS except for specific tests
        Utils.setFakeOSNone()
        proj = ProjectBuilder.builder().build()
    }

    @Test
    void testConstructor() {
        J2objcConfig ext = new J2objcConfig(proj)

        assert proj.file('build/j2objcOutputs/src/main').absolutePath == ext.destSrcMainDir
        assert proj.file('build/j2objcOutputs/src/test').absolutePath == ext.destSrcTestDir
    }

    @Test
    // All variations of main/test and objc/resources
    void testGetDestDirFile_AllVariations() {
        J2objcConfig ext = new J2objcConfig(proj)

        assert proj.file('build/j2objcOutputs/src/main/objc').absolutePath ==
               ext.getDestSrcDirFile('main', 'objc').absolutePath
        assert proj.file('build/j2objcOutputs/src/test/objc').absolutePath ==
               ext.getDestSrcDirFile('test', 'objc').absolutePath
        assert proj.file('build/j2objcOutputs/src/main/resources').absolutePath ==
               ext.getDestSrcDirFile('main', 'resources').absolutePath
        assert proj.file('build/j2objcOutputs/src/test/resources').absolutePath ==
               ext.getDestSrcDirFile('test', 'resources').absolutePath
    }

    @Test
    void testFilenameCollisionCheckIsSet_Default() {
        J2objcConfig ext = new J2objcConfig(proj)
        assert !ext.getFilenameCollisionCheck()
    }

    @Test
    void testFilenameCollisionCheckIsSet_NoPackageDirectories() {
        J2objcConfig ext = new J2objcConfig(proj)
        ext.translateArgs('--no-package-directories')
        assert !ext.forceFilenameCollisionCheck
        assert ext.getFilenameCollisionCheck()
    }

    @Test
    void testFinalConfigure_MacOSX() {
        Utils.setFakeOSMacOSX()
        J2objcConfig ext = new J2objcConfig(proj)

        assert !ext.finalConfigured
        ext.testingOnlyPrepConfigurations()
        ext.finalConfigure()
        assert ext.finalConfigured
    }

    @Test
    void testFinalConfigure_MacOSX_MissingJ2ObjCHome() {
        Utils.setFakeOSMacOSX()
        J2objcConfig ext = new J2objcConfig(proj)

        expectedException.expect(InvalidUserDataException.class)
        expectedException.expectMessage(StringContains.containsString('J2ObjC Home not set'));

        assert !ext.finalConfigured
        ext.testingOnlyPrepConfigurations()
        ext.finalConfigure()
        assert ext.finalConfigured
    }

    @Test
    void testFinalConfigure_LinuxTranslateOnlyMode() {
        Utils.setFakeOSLinux()
        J2objcConfig ext = new J2objcConfig(proj)
        assert !ext.finalConfigured
        ext.translateOnlyMode = true

        ext.testingOnlyPrepConfigurations()
        ext.finalConfigure()
        assert ext.finalConfigured
    }

    @Test
    void testFinalConfigure_WindowsTranslateOnlyMode() {
        Utils.setFakeOSWindows()
        J2objcConfig ext = new J2objcConfig(proj)
        assert !ext.finalConfigured
        ext.translateOnlyMode = true

        ext.testingOnlyPrepConfigurations()
        ext.finalConfigure()
        assert ext.finalConfigured
    }

    @Test
    void testCycleFinderArgs_SimpleConfig() {
        J2objcConfig ext = new J2objcConfig(proj)

        ext.cycleFinderArgs.add('')

        assert ext.cycleFinderArgs == new ArrayList<>([''])
    }

    @Test
    @CompileStatic(TypeCheckingMode.SKIP)
    void testTranslateArgs() {
        J2objcConfig ext = new J2objcConfig(proj)

        // To test similarly to how it would be configured:
        // j2objcConfig {
        //      translateArgs '--no-package-directories'
        //      translateArgs '--prefixes', 'prefixes.properties'
        // }
        ConfigureUtil.configure(
                {
                    translateArgs '--no-package-directories'
                    translateArgs '--prefixes', 'prefixes.properties'
                }, ext)

        List<String> expected = new ArrayList<>(
                ['--no-package-directories', '--prefixes', 'prefixes.properties'])
        assert ext.translateArgs == expected
    }

    @Test
    void testAppendArgs() {
        List<String> args = new ArrayList()
        J2objcConfig.appendArgs(args, 'testArgs', true, '-arg1', '-arg2')

        List<String> expected = Arrays.asList('-arg1', '-arg2')
        assert expected == args
    }

    @Test(expected = InvalidUserDataException.class)
    void testAppendArgs_Spaces() {
        List<String> args = new ArrayList()
        J2objcConfig.appendArgs(args, 'testArgs', true, '-arg1 -arg2')
    }

    @Test
    void testVerifyNoSpaceArgs_Accept() {
        J2objcConfig.verifyArgs('testArgs', true, '-arg1', '-arg2')
    }

    @Test(expected = InvalidUserDataException.class)
    void testVerifyNoSpaceArgs_RejectNull() {
        List<String> args = new ArrayList()
        J2objcConfig.verifyArgs('testArgs', true, null)
    }

    @Test
    void testVerifyNoSpaceArgs_RejectOnlyWhitespace() {
        expectedException.expect(InvalidUserDataException.class)
        expectedException.expectMessage("testArgs is all whitespace: '    '")

        J2objcConfig.verifyArgs('testArgs', true, '    ')
    }

    @Test
    void testVerifyNoSpaceArgs_RejectSpaces() {
        expectedException.expect(InvalidUserDataException.class)
        expectedException.expectMessage('argument should not contain spaces and be written out as distinct entries')
        expectedException.expectMessage("testArgs '-arg1', '-arg2'")

        J2objcConfig.verifyArgs('testArgs', true, '-arg1 -arg2')
    }

    @Test
    void testVerifyNoSpaceArgs_AllowSpaces() {
        J2objcConfig.verifyArgs('testArgs', false, '-arg1 -arg2')
    }
}
