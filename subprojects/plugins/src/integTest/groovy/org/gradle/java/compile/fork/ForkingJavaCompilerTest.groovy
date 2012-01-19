/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.java.compile.fork

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources

import org.junit.Rule

class ForkingJavaCompilerTest extends AbstractIntegrationSpec {
    @Rule TestResources resources = new TestResources()

    def compileGoodCode() {
        expect:
        succeeds("compileJava")
        output.contains("[javac] Compiling 1 source file")
    }
    
    def compileBadCode() {
        expect:
        fails("compileJava")
    }
}
