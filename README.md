# autoTinypng [ ![Download](https://api.bintray.com/packages/coderyu/drawable_auto_compress_plugin/auto_compress/images/download.svg) ](https://bintray.com/coderyu/drawable_auto_compress_plugin/auto_compress/_latestVersion)


#### Overview

可以用来自动化压缩图片资源，每次编译前会先压缩图片资源，已减小apk文件大小

本项目是以一个gralde插件的形式来自动化压缩图片资源

图片压缩算法用的是tinypng的，所以需要用户自己注册tinypng的账号获取key，普通用户每个月可以压缩500张，具体规则可以看tinypng官网

插件会在preBuild之前(也就是正常编译或者打包前都会检查并压缩)根据配置的信息去压缩对应的图片资源，压缩过的图片有做缓存，不会重复压缩



**图片压缩后会覆盖源文件，并且会再根目录下生成一个.tinycache缓存文件，需要一并加入版本管理并上传至仓库，防止多人重复压缩相同文件造成浪费**



接入方式：

根目录build：

```
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'auto_compress:com.coder_yu.plugin:<lastest_version>'
    }
}

```

app模块下可以配置需要压缩的目录和使用的key

```
apply plugin: 'autocompress'

configInfo {
    keys = ['your_key1','your_key2']//可以配置多个，会循环使用
    dirs = [ "app/src/main/res", "module2/src/main/res"]//根目录的相对目录，可以配置多个，不配置默认为'app/src/main/res'
}
```

OK，直接开始吧