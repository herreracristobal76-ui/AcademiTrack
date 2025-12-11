package com.academitrack.app.domain

abstract class Entidad(
    private var id: String,
    private var fechaCreacion: Long = System.currentTimeMillis()
) {
    fun getId(): String = id
    fun getFechaCreacion(): Long = fechaCreacion

    protected fun setId(nuevoId: String) {
        id = nuevoId
    }

    abstract fun validar(): Boolean
    abstract fun obtenerDescripcion(): String

    open fun obtenerResumen(): String {
        return "Entidad ID: $id"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Entidad
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}