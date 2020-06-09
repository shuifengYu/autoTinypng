package com.coder_yu.autocompressimage

import org.gradle.api.Plugin
import org.gradle.api.Project

class CompressPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("configInfo", ConfigInfo.class)
        def taskPreBuild = project.tasks.getByName("preBuild")


        taskPreBuild.doFirst {
            ArrayList<String> keys = project.extensions.configInfo.keys;
            ArrayList<String> dirs = project.extensions.configInfo.dirs;
            println("mKeys=" + keys + ",dirs=" + dirs)
            try {
                new CompressUtil().compress(project.parent.projectDir, dirs, keys)
            } catch (Exception e) {
                e.printStackTrace()
                CompressUtil.sCompressing = false
            }
        }
    }

}