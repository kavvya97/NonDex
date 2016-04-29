/*
The MIT License (MIT)
Copyright (c) 2015 Alex Gyori
Copyright (c) 2015 Owolabi Legunsen
Copyright (c) 2015 Darko Marinov
Copyright (c) 2015 August Shi


Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package edu.illinois.nondex.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import edu.illinois.nondex.common.Configuration;
import edu.illinois.nondex.common.ConfigurationDefaults;
import edu.illinois.nondex.common.Logger;
import edu.illinois.nondex.common.Utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "debug", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class DebugMojo extends AbstractNondexMojo {

    private List<String> executions = new LinkedList<>();
    
    private SetMultimap<String, Configuration> testsFailing = HashMultimap.create();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
        parseExecutions();
        parseTests();
        
        for (String test : testsFailing.keySet()) {
            DebugTask debugging = new DebugTask(test, surefire, originalArgLine,
                    mavenProject, mavenSession, pluginManager, testsFailing.get(test));
            Pair<Integer, Integer> limits = debugging.debug();
            Logger.getGlobal().log(Level.SEVERE, "Limits: " + limits.getLeft() + " : " + limits.getRight());
        }
    }

    private void parseTests() {
        for (String execution : this.executions) {
            Properties props = Utils.openPropertiesFrom(
                    Paths.get(ConfigurationDefaults.NONDEX_DIR, execution, ConfigurationDefaults.CONFIGURATION_FILE));
            Configuration config = Configuration.parseArgs(props);
            for (String test : config.getFailedTests()) {
                this.testsFailing.put(test, config);
            }
        }
    }

    private void parseExecutions() {
        File run = Paths.get(ConfigurationDefaults.NONDEX_DIR, this.runId)
                .toFile();

        try (BufferedReader br = new BufferedReader(new FileReader(run))) {
            String line;
            while ((line = br.readLine()) != null) {
                this.executions.add(line.trim());
            }
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, "Could not open run file to parse executions", ex);
        }
    }

    
}