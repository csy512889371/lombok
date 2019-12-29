package lombok.eclipse.handlers;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.extern.hoppip.HoppipSlf4j;
import org.mangosdk.spi.ProviderFor;
import org.eclipse.jdt.internal.compiler.ast.Annotation;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.eclipse.jdt.internal.compiler.ast.*;
import static lombok.eclipse.Eclipse.fromQualifiedName;
import static lombok.eclipse.handlers.EclipseHandlerUtil.*;
import static lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import static lombok.eclipse.handlers.EclipseHandlerUtil.copyType;
import static lombok.eclipse.handlers.EclipseHandlerUtil.setGeneratedBy;

/**
 * @className HandlerHoppipSlf4j
 * @author: liuzhen
 * @create: 2019-12-23 23:35
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandlerHoppipSlf4j extends EclipseAnnotationHandler<HoppipSlf4j> {
    private static final String FIELD_ClASS_NAME="com.souche.hoppip.logger.Logger";
    private static final String FACTORY_METHOD_RECEIVER="com.souche.hoppip.logger.LoggerManager";
    private static final String FACTORY_METHOD_SELECTOR="getLogger";
    /**
     *
     * @param annotation 注解参数
     * @param source 表示HoppipSlf4j注解的Eclipse ast节点
     * @param annotationNode lombok对source的再一层封装，可以实现source不能实现的功能
     */
    @Override
    public void handle(AnnotationValues<HoppipSlf4j> annotation,final Annotation source,final EclipseNode annotationNode) {
        EclipseNode owner = annotationNode.up();

        if (!checkFieldInject(annotationNode, owner,annotation)) {
            return;
        }

        // 所在类真实的ast节点
        TypeDeclaration typeDecl = null;
        if (owner.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) owner.get();

        String[] names = annotation.getInstance().names();
        String[] params = annotation.getInstance().params();
        for(int i=0;i<names.length;i++){
            String name=names[i].trim();
            FieldDeclaration fieldDeclaration = createField(source, name,params[i]);
            fieldDeclaration.traverse(new lombok.eclipse.handlers.SetGeneratedByVisitor(source), typeDecl.staticInitializerScope);
            injectField(owner, fieldDeclaration);
        }
        owner.rebuild();

    }

    /**
     * 校验是否合法
     * @param annotationNode
     * @param owner
     * @param annotation
     * @return
     */
    private boolean checkFieldInject(final EclipseNode annotationNode,final EclipseNode owner,final AnnotationValues<HoppipSlf4j> annotation){
        if (owner.getKind() != AST.Kind.TYPE) {
            return false;
        }

        TypeDeclaration typeDecl = null;
        if (owner.get() instanceof TypeDeclaration) typeDecl = (TypeDeclaration) owner.get();
        int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;

        boolean notAClass = (modifiers &
                (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation)) != 0;

        if (typeDecl == null || notAClass) {
            annotationNode.addError("@HoppipSlf4j is legal only on classes and enums.");
            return false;
        }

        String[] names = annotation.getInstance().names();
        String[] params = annotation.getInstance().params();
        if(names.length!=params.length){
            annotationNode.addError("@HoppipSlf4j names length not equal to params length");
            return false;
        }
        for(int i=0;i<names.length;i++){
            if(names[i]==null||names[i].trim().length()==0){
                annotationNode.addError("@HoppipSlf4j names is not null or empty");
                return false;
            }
            if(params[i]==null){
                annotationNode.addError("@HoppipSlf4j params is not null or empty");
                return false;
            }
            names[i]=names[i].trim();
        }
        for(int i=0;i<names.length-1;i++){
            for(int j=i+1;j<names.length;j++){
                if(names[i].equals(names[j])){
                    annotationNode.addError("@HoppipSlf4j : duplicate name ["+names[i]+"] not allowed");
                    return false;
                }
            }
        }
        for(String name:names){
            if (fieldExists(name, owner) != MemberExistsResult.NOT_EXISTS) {
                annotationNode.addWarning("Field '" + name + "' already exists.");
                return false;
            }
        }
        return true;
    }

    /**
     * 创建节点
     * @param source
     * @param loggingType
     * @param loggerTopic
     * @return
     */
    private static FieldDeclaration createField(Annotation source, String name, String param) {
        int pS = source.sourceStart, pE = source.sourceEnd;
        long p = (long) pS << 32 | pE;

        FieldDeclaration fieldDecl = new FieldDeclaration(name.toCharArray(), 0, -1);
        setGeneratedBy(fieldDecl, source);
        fieldDecl.declarationSourceEnd = -1;
        fieldDecl.modifiers = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;

        fieldDecl.type = createTypeReference(FIELD_ClASS_NAME, source);

        MessageSend factoryMethodCall = new MessageSend();
        setGeneratedBy(factoryMethodCall, source);

        factoryMethodCall.receiver = createNameReference(FACTORY_METHOD_RECEIVER, source);
        factoryMethodCall.selector = FACTORY_METHOD_SELECTOR.toCharArray();

        Expression parameter = new StringLiteral(param.toCharArray(), pS, pE, 0);


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
