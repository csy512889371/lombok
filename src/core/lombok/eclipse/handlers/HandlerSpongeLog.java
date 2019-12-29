package lombok.eclipse.handlers;

import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.mangosdk.spi.ProviderFor;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import lombok.extern.sponge.SpongeLog;
/**
 * @className HandlerSpongeLog
 * @author: liuzhen
 * @create: 2019-12-21 17:27
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandlerSpongeLog extends EclipseAnnotationHandler<SpongeLog>{

    private static final String LOG_FIELD_NAME = "spongeLog";

    @Override
    public void handle(AnnotationValues<SpongeLog> annotation,final Annotation source, final EclipseNode annotationNode) {
        EclipseNode owner = annotationNode.up();

        if (owner.getKind() != AST.Kind.TYPE) {
            return;
        }

        TypeDeclaration typeDecl = null;
        if (owner.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) owner.get();
        int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;

        boolean notAClass = (modifiers &
                (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation)) != 0;

        if (typeDecl == null || notAClass) {
            annotationNode.addError("@SpongeLog is legal only on classes and enums.");
            return;
        }

        if (fieldExists(LOG_FIELD_NAME, owner) != MemberExistsResult.NOT_EXISTS) {
            annotationNode.addWarning("Field '" + LOG_FIELD_NAME + "' already exists.");
            return;
        }

        ClassLiteralAccess loggingType = selfType(owner, source);

        FieldDeclaration fieldDeclaration = createField(source, loggingType, annotation.getInstance().topic());
        fieldDeclaration.traverse(new lombok.eclipse.handlers.SetGeneratedByVisitor(source), typeDecl.staticInitializerScope);
        injectField(owner, fieldDeclaration);
        owner.rebuild();
    }

    private static ClassLiteralAccess selfType(EclipseNode type, Annotation source) {
        int pS = source.sourceStart, pE = source.sourceEnd;
        long p = (long) pS << 32 | pE;

        TypeDeclaration typeDeclaration = (TypeDeclaration) type.get();
        TypeReference typeReference = new SingleTypeReference(typeDeclaration.name, p);
        setGeneratedBy(typeReference, source);

        ClassLiteralAccess result = new ClassLiteralAccess(source.sourceEnd, typeReference);
        setGeneratedBy(result, source);

        return result;
    }

    private static FieldDeclaration createField(Annotation source, ClassLiteralAccess loggingType, String loggerTopic) {
        int pS = source.sourceStart, pE = source.sourceEnd;
        long p = (long) pS << 32 | pE;

        FieldDeclaration fieldDecl = new FieldDeclaration(LOG_FIELD_NAME.toCharArray(), 0, -1);
        setGeneratedBy(fieldDecl, source);
        fieldDecl.declarationSourceEnd = -1;
        fieldDecl.modifiers = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;

        fieldDecl.type = createTypeReference("org.slf4j.Logger", source);

        MessageSend factoryMethodCall = new MessageSend();
        setGeneratedBy(factoryMethodCall, source);

        factoryMethodCall.receiver = createNameReference("org.slf4j.LoggerFactory", source);
        factoryMethodCall.selector = "getLogger".toCharArray();

        Expression parameter = null;
        if (loggerTopic == null || loggerTopic.trim().length() == 0) {
            TypeReference copy = copyType(loggingType.type, source);
            parameter = new ClassLiteralAccess(source.sourceEnd, copy);
            setGeneratedBy(parameter, source);
        } else {
            parameter = new StringLiteral(loggerTopic.toCharArray(), pS, pE, 0);
        }

        factoryMethodCall.arguments = new Expression[]{parameter};
        factoryMethodCall.nameSourcePosition = p;
        factoryMethodCall.sourceStart = pS;
        factoryMethodCall.sourceEnd = factoryMethodCall.statementEnd = pE;

        fieldDecl.initialization = factoryMethodCall;

        return fieldDecl;
    }

    public static TypeReference createTypeReference(String typeName, Annotation source) {
        int pS = source.sourceStart, pE = source.sourceEnd;
        long p = (long) pS << 32 | pE;

        TypeReference typeReference;
        if (typeName.contains(".")) {

            char[][] typeNameTokens = fromQualifiedName(typeName);
            long[] pos = new long[typeNameTokens.length];
            Arrays.fill(pos, p);

            typeReference = new QualifiedTypeReference(typeNameTokens, pos);
        } else {
            typeReference = null;
        }

        setGeneratedBy(typeReference, source);
        return typeReference;
    }

}
