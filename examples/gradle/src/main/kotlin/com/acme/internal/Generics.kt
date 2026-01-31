package com.acme.internal

import dev.packageprivate.PackagePrivate

/**
 * Class used in generic type parameters - only used within this package.
 */
@PackagePrivate
class InternalEntity(val id: Int, val name: String)

/**
 * Repository using InternalEntity in generics.
 */
@PackagePrivate
class InternalRepository {
    private val items: MutableList<InternalEntity> = mutableListOf()
    private val index: Map<Int, InternalEntity> = emptyMap()
    
    fun add(entity: InternalEntity) {
        items.add(entity)
    }
    
    fun getAll(): List<InternalEntity> = items.toList()
}
