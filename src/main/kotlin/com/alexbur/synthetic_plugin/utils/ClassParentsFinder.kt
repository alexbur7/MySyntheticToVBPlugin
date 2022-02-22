package com.alexbur.synthetic_plugin.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import com.jetbrains.rd.util.qualifiedName
import org.jetbrains.kotlin.psi.KtPsiFactory

class ClassParentsFinder(psiClass: PsiClass) {

    private val parents = InheritanceUtil.getSuperClasses(psiClass)

    val typeVb: TypeVB?
        get() {
            return when (parents.firstOrNull()?.qualifiedName) {
                TypeVB.COLLAPSING_VB.oldName -> TypeVB.COLLAPSING_VB
                TypeVB.REFRESH_VB.oldName -> TypeVB.REFRESH_VB
                TypeVB.LIFTABLE_VB.oldName -> TypeVB.LIFTABLE_VB
                TypeVB.BASE_VB.oldName -> TypeVB.BASE_VB
                else -> null
            }
        }

    fun isChildOf(vararg classQualifiedNames: String): Boolean {
        return parents.any { parentClass ->
            parentClass.qualifiedName in classQualifiedNames
        }
    }

    enum class TypeVB(val oldName: String, val newName: String) {
        BASE_VB("com.nlmk.mcs.presentation_layer.base.BaseFragment", "BaseVBFragment"),
        COLLAPSING_VB("com.nlmk.mcs.presentation_layer.base.CollapsingTitleFragment", "CollapsingTitleVBFragment"),
        REFRESH_VB("com.nlmk.mcs.presentation_layer.base.BaseRefreshFragment", "BaseRefreshVBFragment"),
        LIFTABLE_VB("com.nlmk.mcs.presentation_layer.base.LiftableTitleFragment","LiftableTitleVBFragment")
    }
}
