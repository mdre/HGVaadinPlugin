package com.github.mdre.hgvaadinplugin

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.github.dkorotych.gradle.maven.exec.MavenExec;

class HGVaadinPlugin implements Plugin<Project> {
    def projectRef

    @Override
    void apply(Project project) {
        println "Hybrid Gradle Vaaadin plugin."
        
        project.plugins.apply('com.github.dkorotych.gradle-maven-exec')
        project.plugins.apply('war')
        
        project.configure(project) {
            sourceSets {
                main {
                    resources {
                        srcDirs += [
                            'target/classes',
                        ]
                    }
                }
            }
            war {
                println "war +++++++++++++++++"
                // from "$buildDir/classes/java/main"
                webInf {
                    println "webinf"
                    from ("target/classes/META-INF") {
                        into "classes/META-INF"
                    }
                }
            }
        }

        HGVaadinConfig hgvConfig = project.extensions.create('hgvConfig', HGVaadinConfig.class)

        // project.getPluginManager().apply('gradle-maven-exec-plugin')
        projectRef = project

        //verify if pom exist
        verifyPomExist()

        project.task('prepareFrontEnd', type: MavenExec) {
            group = "Hybrid Gradle Vaadin plugin"
            description = "prepare front-end"

            goals 'vaadin:prepare-frontend'
        }

        project.task('buildFrontEnd', type: MavenExec){
            group = "Hybrid Gradle Vaadin plugin"
            description = "build front-end"

            goals 'vaadin:build-frontend'
            //finalizedBy(project.tasks['war'])
        }

        project.task('vaadinBuild'){
            group = "Hybrid Gradle Vaadin plugin"
            description = "build the app"

            finalizedBy(project.tasks['build'])
            doLast {
                println "***************************************************************"
                println "Building Vaadin project: " + project.name
                println "VERSION: " + project.version
                println "***************************************************************"
                printDep()
            }
        }

        project.task("dbg") {
            doLast {
                println project
                println project.version
                println project.name
                println hgvConfig.getVaadinVersion()

                def pomxml = new XmlParser().parse('pom.xml')

                println pomxml.properties
            }
        }

        project.task('vaadinClean'){
            group = "Hybrid Gradle Vaadin plugin"
            description = "clean the project"

            doLast {
                project.delete("bin")
                project.delete("build")
                project.delete("node_modules")
                project.delete("target")
                project.delete("package.json")
                project.delete("webpack.config.js")
                project.delete("webpack.generated.js")
            }
        }

        project.tasks['classes'].doLast {
            project.copy {
                from "build/classes/java/main/."
                into "target/classes"
            }
        }        
        
        project.tasks['war'].doFirst {
            println "war first <<<<<<<<<<<<<<<<<<<<<<<<<<<"
            // project.copy {
            //     from "build/classes/java/main/."
            //     into "target/classes"
            // }
        }        
        project.tasks['war'].doLast {
            println "war last <<<<<<<<<<<<<<<<<<<<<<<<<<<"
            // project.copy {
            //     from "build/classes/java/main/."
            //     into "target/classes"
            // }
        }        
        
        project.tasks['build'].doLast {
            println "build <<<<<<<<<<<<<<<<<<<<<<<<<<<"
            // project.copy {
            //     from "build/classes/java/main/."
            //     into "target/classes"
            // }
        }        
        

        //project.tasks['war'].doFirst(project.tasks['buildFrontEnd'])

        project.task('updatePom') {
            group = "Hybrid Gradle Vaadin plugin"
            description = "update the project pom"
            doLast {
                def pomxml = new XmlParser().parse('pom.xml')

                // actualizar la versión de vaadin
                pomxml."properties".'vaadin.version'[0].setValue( hgvConfig.getVaadinVersion())
                pomxml."properties".'maven.compiler.source'[0].setValue( hgvConfig.getCompilerSource())
                pomxml."properties".'maven.compiler.target'[0].setValue( hgvConfig.getCompilerTarget())
                pomxml."properties".'project.build.sourceEncoding'[0].setValue( hgvConfig.getSourceEncoding())

                // borrar todas las dependencias
                pomxml.remove(pomxml.dependencies)

                // agregar las dependencias desde gradle
                def dependencies = new Node(pomxml,'dependencies')
                project.configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
                    println dep

                    def depGroup = dep.getModuleGroup()
                    def depName = dep.getModuleName()
                    def depVersion = dep.getModuleVersion()
                    
                    // agregar un nodo para cada dependencia
                    def depInstance = new Node(dependencies, "dependency")
                    new Node(depInstance,"groupId","$depGroup")
                    new Node(depInstance,"artifactId","$depName")
                    new Node(depInstance,"version","$depVersion")
                    
                }

                //Save File
                def writer = new FileWriter('pom.xml')
                groovy.xml.XmlUtil.serialize(pomxml, writer)
            }

        }

        project.afterEvaluate {
            project.tasks['updatePom'].dependsOn project.tasks['classes']
            project.tasks['prepareFrontEnd'].dependsOn project.tasks['updatePom']
            project.tasks['buildFrontEnd'].dependsOn project.tasks['prepareFrontEnd']
            project.tasks['vaadinBuild'].dependsOn project.tasks['buildFrontEnd']
            project.tasks['vaadinClean'].dependsOn project.tasks['clean']
        }
    }

    
    def printDep (){
        println("Project dependencies: ")
        println "--------------------------------------------------------------"
        projectRef.configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies.each { dep ->
            println dep
        }
        println "--------------------------------------------------------------"
    }

    def verifyPomExist() {
        File pomExist = new File("pom.xml")
        
        if (!pomExist.exists()) {
            String templ = getClass().getResourceAsStream("/template/pom_template.xml").text
            File output = new File("pom.xml")
            output << templ
        } 
    }

    
}
