package cn.langya.lmixin

import java.io.File

class MappingParser(srgFilePath: String) {

    private val classMap = mutableMapOf<String, String>()
    private val fieldMap = mutableMapOf<String, String>()
    private val methodMap = mutableMapOf<String, String>()

    init {
        File(srgFilePath).forEachLine { line ->
            when {
                line.startsWith("CL:") -> {
                    val parts = line.split(" ")
                    classMap[parts[1]] = parts[2]
                }

                line.startsWith("FD:") -> {
                    val parts = line.split(" ")
                    fieldMap[parts[1]] = parts[2]
                }

                line.startsWith("MD:") -> {
                    val parts = line.split(" ")
                    methodMap[parts[1] + parts[2]] = parts[3] + parts[4]
                }
            }
        }
    }

    fun mapClassName(obfName: String): String {
        return classMap[obfName] ?: obfName
    }

    fun mapFieldName(owner: String, obfName: String): String {
        return fieldMap["$owner/$obfName"] ?: obfName
    }

    fun mapMethodName(owner: String, obfName: String, obfDesc: String): String {
        return methodMap["$owner/$obfName$obfDesc"]?.substringBefore("(")?.substringAfterLast("/") ?: obfName
    }

    // TODO 待实现
    fun mapMethodDesc(obfDesc: String): String {
        return obfDesc
    }
}