package com.alexbur.synthetic_plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import com.alexbur.synthetic_plugin.delegates.ConvertKtFileDelegate
import com.alexbur.synthetic_plugin.extensions.androidFacet

class ConvertSyntheticToViewBindingAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE) as KtFile
        val project = e.project as Project

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        project.executeWriteCommand(COMMAND_NAME) {
            ConvertKtFileDelegate.perform(file, project, e.androidFacet())
        }
        //Messages.showMessageDialog("message", "title", null)
    }

    private companion object {
        const val COMMAND_NAME = "ConvertSyntheticsToViewBindingCommand"
    }
}