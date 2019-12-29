package lombok.javac.handlers;

import com.sun.tools.javac.tree.JCTree;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;
import lombok.extern.hoppip.HoppipSlf4j;
import org.mangosdk.spi.ProviderFor;
import static lombok.javac.handlers.JavacHandlerUtil.*;

/**
 * @className HandlerHoppipSlf4j
 * @author: liuzhen
 * @create: 2019-12-23 23:00
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandlerHoppipSlf4j extends JavacAnnotationHandler<HoppipSlf4j>  {

    private static final String FIELD_ClASS_NAME="com.souche.hoppip.logger.Logger";
    private static final String FACTORY_METHOD_EXPRESS="com.souche.hoppip.logger.LoggerManager.getLogger";

    @Override
    public void handle(AnnotationValues<HoppipSlf4j> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        JavacNode typeNode = annotationNode.up();
        if (!checkFieldInject(annotationNode, typeNode,annotation)) {
            return;
        }

        String[] names = annotation.getInstance().names();
        String[] params = annotation.getInstance().params();
        for(int i=0;i<names.length;i++){
            String name=names[i].trim();

            JCTree.JCVariableDecl fieldDecl = createField(annotation, annotationNode, typeNode,name,params[i]);
            injectFieldAndMarkGenerated(typeNode, fieldDecl);
        }
    }

    private JCTree.JCVariableDecl createField(final AnnotationValues<HoppipSlf4j> annotation, final JavacNode annotationNode, final JavacNode typeNode,String fieldNameStr,String param) {
        JavacTreeMaker maker = typeNode.getTreeMaker();
        Name name = ((JCTree.JCClassDecl) typeNode.get()).name;
        JCTree.JCLiteral paramLiteral = maker.Literal(param);

        JCTree.JCExpression loggerType = chainDotsString(typeNode, FIELD_ClASS_NAME);
        JCTree.JCExpression factoryMethod = chainDotsString(typeNode, FACTORY_METHOD_EXPRESS);

        JCTree.JCExpression paramExpression =paramLiteral;


        JCTree.JCMethodInvocation factoryMethodCall = maker.Apply(List.<JCTree.JCExpression>nil(), factoryMethod, paramExpression != null ? List.of(paramExpression) : List.<JCTree.JCExpression>nil());

        return recursiveSetGeneratedBy(maker.VarDef(
                maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC ),
                typeNode.toName(fieldNameStr), loggerType, factoryMethodCall)
                , annotationNode.get(), typeNode.getContext());
    }


    private boolean checkFieldInject(final JavacNode annotationNode, final JavacNode typeNode,final AnnotationValues<HoppipSlf4j> annotation) {
        if (typeNode.getKind() != AST.Kind.TYPE) {
            annotationNode.addError("@HoppipSlf4j is legal only on types.");
            return false;
        }
        if ((((JCTree.JCClassDecl)typeNode.get()).mods.flags & Flags.INTERFACE) != 0) {
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
            if (fieldExists(name, typeNode) != MemberExistsResult.NOT_EXISTS) {
                annotationNode.addWarning("Field '" + name + "' already exists.");
                return false;
            }
        }

        return true;
    }
}
