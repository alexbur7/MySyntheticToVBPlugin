package com.alexbur.synthetic_plugin.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil

class ClassParentsFinder(psiClass: PsiClass) {

    private val parents = InheritanceUtil.getSuperClasses(psiClass)

    val oldParentFragment = parents?.firstOrNull()?.qualifiedName?.substringAfterLast(".")

    val newParentFragment = oldParentFragment?.replace("Fragment", "VBFragment")

    fun isChildOf(vararg classQualifiedNames: String): Boolean {
        return parents.any { parentClass ->
            parentClass.qualifiedName in classQualifiedNames
        }
    }
}

fun String?.isNeedGeneric(): Boolean {
    return when (this) {
        "BaseVBFragment", "CollapsingTitleVBFragment", "BaseRefreshVBFragment",
        "CenterTitleVBFragment", "LargeTitleVBFragment",
        "LiftableTitleVBFragment" -> true
        else -> false
    }
}
