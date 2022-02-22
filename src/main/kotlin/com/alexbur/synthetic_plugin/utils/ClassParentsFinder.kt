package com.alexbur.synthetic_plugin.utils

import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil

class ClassParentsFinder(psiClass: PsiClass) {

    private val parents = InheritanceUtil.getSuperClasses(psiClass)

    val typeVb: TypeVB?
        get() {
            return when (parents.firstOrNull()?.qualifiedName) {
                BASE_FRAGMENT_IMPORT + TypeVB.BASE_VB.oldName -> TypeVB.BASE_VB
                BASE_FRAGMENT_IMPORT + TypeVB.REFRESH_VB.oldName -> TypeVB.REFRESH_VB
                BASE_FRAGMENT_IMPORT + TypeVB.LIFTABLE_VB.oldName -> TypeVB.LIFTABLE_VB
                BASE_FRAGMENT_IMPORT + TypeVB.CENTER_TITLE_VB.oldName -> TypeVB.CENTER_TITLE_VB
                BASE_FRAGMENT_IMPORT + TypeVB.COLLAPSING_TITLE_RECYCLER_VB.oldName -> TypeVB.COLLAPSING_TITLE_RECYCLER_VB
                BASE_FRAGMENT_IMPORT + TypeVB.COLLAPSING_TITLE_VIEW_PAGER_VB.oldName -> TypeVB.COLLAPSING_TITLE_VIEW_PAGER_VB
                BASE_FRAGMENT_IMPORT + TypeVB.COLLAPSING_TITLE_REFRESH_RECYCLER_VB.oldName -> TypeVB.COLLAPSING_TITLE_REFRESH_RECYCLER_VB
                BASE_FRAGMENT_IMPORT + TypeVB.COLLAPSING_TITLE_REFRESH_RECYCLER_WITH_BUTTON_VB.oldName -> TypeVB.COLLAPSING_TITLE_REFRESH_RECYCLER_WITH_BUTTON_VB
                BASE_FRAGMENT_IMPORT + TypeVB.LARGE_TITLE_VB.oldName -> TypeVB.LARGE_TITLE_VB
                BASE_FRAGMENT_IMPORT + TypeVB.COLLAPSING_VB.oldName -> TypeVB.COLLAPSING_VB
                else -> null
            }
        }

    fun isChildOf(vararg classQualifiedNames: String): Boolean {
        return parents.any { parentClass ->
            parentClass.qualifiedName in classQualifiedNames
        }
    }

    enum class TypeVB(val oldName: String, val newName: String) {
        BASE_VB("BaseFragment", "BaseVBFragment"),
        COLLAPSING_VB("CollapsingTitleFragment", "CollapsingTitleVBFragment"),
        REFRESH_VB("BaseRefreshFragment", "BaseRefreshVBFragment"),
        LIFTABLE_VB("LiftableTitleFragment","LiftableTitleVBFragment"),
        CENTER_TITLE_VB("CenterTitleFragment","CenterTitleVBFragment"),
        COLLAPSING_TITLE_RECYCLER_VB("CollapsingTitleRecyclerFragment","CollapsingTitleRecyclerVBFragment"),
        COLLAPSING_TITLE_REFRESH_RECYCLER_VB("CollapsingTitleRefreshRecyclerFragment","CollapsingTitleRefreshRecyclerVBFragment"),
        COLLAPSING_TITLE_REFRESH_RECYCLER_WITH_BUTTON_VB("CollapsingTitleRefreshRecyclerWithButtonFragment","CollapsingTitleRefreshRecyclerWithButtonVBFragment"),
        COLLAPSING_TITLE_VIEW_PAGER_VB("CollapsingTitleViewPagerFragment","CollapsingTitleViewPagerFragment"),
        LARGE_TITLE_VB("LargeTitleFragment","LargeTitleFragment"),
    }

    private companion object{
        const val BASE_FRAGMENT_IMPORT = "com.nlmk.mcs.presentation_layer.base."
    }
}
