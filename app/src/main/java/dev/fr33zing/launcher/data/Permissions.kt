package dev.fr33zing.launcher.data

enum class PermissionScope {
    Self,
    Recursive,
}

enum class PermissionKind {
    Create,
    Delete,
    Edit,
    Move,
}

typealias PermissionMap = Map<PermissionKind, MutableSet<PermissionScope>>

fun PermissionMap.hasPermission(kind: PermissionKind, scope: PermissionScope): Boolean {
    return (this[kind] ?: return false).contains(scope)
}

fun PermissionMap.clone(): PermissionMap = keys.associateWith { key -> this[key]!!.toMutableSet() }

private val allScopes = PermissionScope.values().toMutableSet()
val AllPermissions: PermissionMap = PermissionKind.values().associateWith { allScopes }
