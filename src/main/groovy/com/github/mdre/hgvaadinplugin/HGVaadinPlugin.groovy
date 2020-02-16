package com.github.mdre.hgvaadinplugin

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import com.github.dkorotych.gradle.maven.exec.MavenExec;

// class HGVaadinConfig {
//     // default vaadin version
//     String vaadinVersion = "14.0.0"
// }

class HGVaadinPlugin implements Plugin<Project> {
    def projectRef

    @Override
    void apply(Project project) {
        println "Hybrid Gradle Vaaadin plugin."
        
        project.plugins.apply('com.github.dkorotych.gradle-maven-exec')
        
        HGVaadinConfig hgvConfig = project.extensions.create('hgvConfig', HGVaadinConfig.class)

        // project.getPluginManager().apply('gradle-maven-exec-plugin')
        projectRef = project

        project.task('prepareFrontEnd', type: MavenExec) {
            group = "Vaadin mvn build"
            description = "prepare front-end"

            goals 'vaadin:prepare-frontend'
        }

        project.task('buildFrontEnd', type: MavenExec){
             goals 'vaadin:build-frontend'
        }

        project.task('vaadinBuild'){
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

        project.tasks['build'].doLast {
            project.copy {
                from "build/classes/java/main/."
                into "target/classes"
            }
        }        

        project.task('updatePom') {
            doLast {
                def pomxml = new XmlParser().parse('pom.xml')

                // actualizar la versión de vaadin
                pomxml."properties".'vaadin.version'[0].setValue( hgvConfig.getVaadinVersion())

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

                //Option 1: Write XML all on one line
                // def builder = new groovy.xml.StreamingMarkupBuilder()
                // builder.encoding = "UTF-8"
                // writer << builder.bind {
                //     mkp.yield pomxml
                // }

                //Option 2: Pretty print XML
                groovy.xml.XmlUtil.serialize(pomxml, writer)
            }

        }

        project.afterEvaluate {
            project.tasks['updatePom'].dependsOn project.tasks['build']
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


    // def copyCompiledClass() {
    //     copy {
    //         from "build/classes/java/main/."
    //         into "target/classes"
    //     }
    // }
}
