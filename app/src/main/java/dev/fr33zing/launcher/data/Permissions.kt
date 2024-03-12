package dev.fr33zing.launcher.data

import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload

enum class PermissionScope {
    Self,
    Recursive,
}

enum class PermissionKind {
    Create,
    Delete,
    Edit,
    Move,
    MoveIn,
    MoveOut,
}

typealias PermissionMap = Map<PermissionKind, MutableSet<PermissionScope>>

fun PermissionMap.hasPermission(
    kind: PermissionKind,
    scope: PermissionScope,
): Boolean {
    return (this[kind] ?: return false).contains(scope)
}

fun PermissionMap.clone(): PermissionMap = keys.associateWith { key -> this[key]!!.toMutableSet() }

fun PermissionMap.adjustOwnPermissions(payload: Payload): PermissionMap =
    clone().also { permissions ->
        if (payload !is Directory) return@also

        PermissionKind.entries.forEach { kind ->
            PermissionScope.entries.forEach { scope ->
                if (!payload.hasPermission(kind, scope)) permissions[kind]!!.remove(scope)
            }
        }
    }

fun PermissionMap.adjustChildPermissions(payload: Payload): PermissionMap =
    clone().also { permissions ->
        if (payload !is Directory) return@also

        PermissionKind.entries.forEach { kind ->
            if (!payload.hasPermission(kind, PermissionScope.Recursive)) {
                permissions[kind]!!.remove(PermissionScope.Self)
                permissions[kind]!!.remove(PermissionScope.Recursive)
            }
        }
    }

private val allScopes = PermissionScope.entries.toTypedArray().toMutableSet()
val AllPermissions: PermissionMap = PermissionKind.entries.associateWith { allScopes }
