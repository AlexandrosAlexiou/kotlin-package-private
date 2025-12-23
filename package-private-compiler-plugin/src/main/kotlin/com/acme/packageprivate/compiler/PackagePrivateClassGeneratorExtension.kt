package com.acme.packageprivate.compiler

import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.*

/**
 * Modifies JVM bytecode to make @PackagePrivate declarations truly package-private.
 *
 * In JVM bytecode:
 * - public = ACC_PUBLIC flag set
 * - package-private = no access flag (0)
 * - protected = ACC_PROTECTED flag set
 * - private = ACC_PRIVATE flag set
 *
 * This extension removes the ACC_PUBLIC flag from declarations marked @PackagePrivate.
 */
class PackagePrivateClassGeneratorExtension : ClassGeneratorExtension {

    companion object {
        private val PACKAGE_PRIVATE_FQ_NAME = FqName("com.acme.packageprivate.PackagePrivate")
    }

    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator {
        return PackagePrivateClassGenerator(generator, declaration)
    }

    private class PackagePrivateClassGenerator(
        private val delegate: ClassGenerator,
        irClass: IrClass?,
    ) : ClassGenerator by delegate {

        private val isClassPackagePrivate = irClass?.hasAnnotation(PACKAGE_PRIVATE_FQ_NAME) == true

        override fun defineClass(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String,
            interfaces: Array<out String>,
        ) {
            val modifiedAccess =
                if (isClassPackagePrivate) {
                    // Remove ACC_PUBLIC to make it package-private
                    access and Opcodes.ACC_PUBLIC.inv()
                } else {
                    access
                }
            delegate.defineClass(version, modifiedAccess, name, signature, superName, interfaces)
        }

        override fun newMethod(
            declaration: IrFunction?,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val isMethodPackagePrivate = declaration?.hasAnnotation(PACKAGE_PRIVATE_FQ_NAME) == true

            val modifiedAccess =
                if (isMethodPackagePrivate) {
                    // Remove ACC_PUBLIC to make it package-private
                    access and Opcodes.ACC_PUBLIC.inv()
                } else {
                    access
                }

            return delegate.newMethod(
                declaration,
                modifiedAccess,
                name,
                desc,
                signature,
                exceptions,
            )
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun newField(
            declaration: IrField?,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            value: Any?,
        ): FieldVisitor {
            // Check if the property (parent of field) has @PackagePrivate
            val isFieldPackagePrivate =
                declaration
                    ?.correspondingPropertySymbol
                    ?.owner
                    ?.hasAnnotation(PACKAGE_PRIVATE_FQ_NAME) == true

            val modifiedAccess =
                if (isFieldPackagePrivate) {
                    // Remove ACC_PUBLIC to make it package-private
                    access and Opcodes.ACC_PUBLIC.inv()
                } else {
                    access
                }

            return delegate.newField(declaration, modifiedAccess, name, desc, signature, value)
        }
    }
}
