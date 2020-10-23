/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.expectactual.CreateActualClassFix
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.oldFashionedDescription
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.classOrObjectVisitor
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker.Companion.allStrongIncompatibilities
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

class OptionalExpectationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return classOrObjectVisitor(fun(classOrObject: KtClassOrObject) {
            if (classOrObject !is KtClass || !classOrObject.isAnnotation()) return
            if (!classOrObject.hasExpectModifier()) return

            val descriptor = classOrObject.resolveToDescriptorIfAny() ?: return
            if (!descriptor.annotations.hasAnnotation(ExpectedActualDeclarationChecker.OPTIONAL_EXPECTATION_FQ_NAME)) return

            // FIXME(dsavvinov): this is wrong in HMPP model, use logic similar to ExpectedActualDeclarationChecker
            val implementingModules = classOrObject.findModuleDescriptor().implementingDescriptors
            if (implementingModules.isEmpty()) return

            for (actualModuleDescriptor in implementingModules) {
                val compatibility = ExpectedActualResolver.findActualForExpected(descriptor, actualModuleDescriptor) ?: continue
                if (!compatibility.allStrongIncompatibilities() &&
                    (ExpectedActualResolver.Compatibility.Compatible in compatibility ||
                            !compatibility.values.flatMapTo(
                                hashSetOf()
                            ) { it }.all { actual ->
                                val expectedOnes = ExpectedActualResolver.findExpectedForActual(actual, descriptor.module)
                                expectedOnes != null && ExpectedActualResolver.Compatibility.Compatible in expectedOnes.keys
                            })
                ) continue
                val platform = actualModuleDescriptor.platform ?: continue
                if (platform.isCommon()) continue

                val displayedName = actualModuleDescriptor.getCapability(ModuleInfo.Capability)?.displayedName ?: ""
                val actualModule = (actualModuleDescriptor.getCapability(ModuleInfo.Capability) as? ModuleSourceInfo)?.module ?: continue
                holder.registerProblem(
                    classOrObject.nameIdentifier ?: classOrObject,
                    KotlinBundle.message(
                        "optionally.expected.annotation.has.no.actual.annotation.in.module.0.for.platform.1",
                        displayedName,
                        platform.oldFashionedDescription
                    ),
                    IntentionWrapper(CreateActualClassFix(classOrObject, actualModule, platform), classOrObject.containingFile)
                )
            }
        })
    }
}