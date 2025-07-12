package cn.langya.lmixin

import java.io.File

class MappingParser(srgFilePath: String) {

    private val classMap = mutableMapOf<String, String>()
    private val fieldMap = mutableMapOf<String, String>()
    private val methodMap = mutableMapOf<String, String>()

    init {
        loadMappings(srgFilePath)
    }
    
    /**
     * 默认构造函数，使用默认的mappings.srg文件
     */
    constructor() : this("mappings.srg")
    
    /**
     * 加载映射文件
     * @param srgFilePath SRG映射文件路径
     */
    private fun loadMappings(srgFilePath: String) {
        val file = File(srgFilePath)
        if (!file.exists()) {
            println("Warning: Mapping file $srgFilePath does not exist")
            return
        }
        
        file.forEachLine { line ->
            when {
                line.startsWith("CL:") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 3) {
                        classMap[parts[1]] = parts[2]
                    }
                }

                line.startsWith("FD:") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 3) {
                        fieldMap[parts[1]] = parts[2]
                    }
                }

                line.startsWith("MD:") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 5) {
                        methodMap[parts[1] + parts[2]] = parts[3] + parts[4]
                    }
                }
            }
        }
    }

    fun mapClassName(obfName: String): String {
        return classMap[obfName] ?: obfName
    }

    fun mapFieldName(owner: String, obfName: String): String {
        return fieldMap["$owner/$obfName"]?.substringAfterLast("/") ?: obfName
    }

    fun mapMethodName(owner: String, obfName: String, obfDesc: String): String {
        return methodMap["$owner/$obfName$obfDesc"]?.substringBefore("(")?.substringAfterLast("/") ?: obfName
    }

    // TODO 待实现
    fun mapMethodDesc(obfDesc: String): String {
        return obfDesc
    }
    
    /**
     * 获取类映射数量
     * @return Int 类映射数量
     */
    fun getClassMapSize(): Int {
        return classMap.size
    }
    
    /**
     * 获取字段映射数量
     * @return Int 字段映射数量
     */
    fun getFieldMapSize(): Int {
        return fieldMap.size
    }
    
    /**
     * 获取方法映射数量
     * @return Int 方法映射数量
     */
    fun getMethodMapSize(): Int {
        return methodMap.size
    }
    
    /**
     * 检查是否包含指定的类映射
     * @param obfName 混淆名称
     * @return Boolean 是否包含
     */
    fun hasClassMapping(obfName: String): Boolean {
        return classMap.containsKey(obfName)
    }
    
    /**
     * 检查是否包含指定的字段映射
     * @param owner 所有者
     * @param obfName 混淆名称
     * @return Boolean 是否包含
     */
    fun hasFieldMapping(owner: String, obfName: String): Boolean {
        return fieldMap.containsKey("$owner/$obfName")
    }
    
    /**
     * 检查是否包含指定的方法映射
     * @param owner 所有者
     * @param obfName 混淆名称
     * @param obfDesc 混淆描述符
     * @return Boolean 是否包含
     */
    fun hasMethodMapping(owner: String, obfName: String, obfDesc: String): Boolean {
        return methodMap.containsKey("$owner/$obfName$obfDesc")
    }
}