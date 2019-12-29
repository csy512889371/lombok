package lombok.javac.handlers;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import lombok.core.AST;
import lombok.core.AnnotationValues;
import lombok.extern.sponge.SpongeLog;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.JavacTreeMaker;

import org.mangosdk.spi.ProviderFor;

import static lombok.javac.handlers.JavacHandlerUtil.*;

/**
 * @className HandlerSpongeLog
 * @author: liuzhen
 * @create: 2019-12-13 20:56
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandlerSpongeLog extends JavacAnnotationHandler<SpongeLog> {
    private static final String LOG_FIELD_NAME = "spongeLog";

    @Override
    public void handle(AnnotationValues<SpongeLog> annotation, JCTree.JCAnnotation ast, JavacNode annotationNode) {
        JavacNode typeNode = annotationNode.up();
        if (!checkFieldInject(annotationNode, typeNode)) {
            return;
        }

        JCTree.JCVariableDecl fieldDecl = createField(annotation, annotationNode, typeNode);


        injectFieldAndMarkGenerated(typeNode, fieldDecl);
    }

    private boolean checkFieldInject(final JavacNode annotationNode, final JavacNode typeNode) {
        if (typeNode.getKind() != AST.Kind.TYPE) {
            annotationNode.addError("@SpongeLog is legal only on types.");
            return false;
        }
        if ((((JCTree.JCClassDecl)typeNode.get()).mods.flags & Flags.INTERFACE) != 0) {
            annotationNode.addError("@SpongeLog is legal only on classes and enums.");
            return false;
        }
        if (fieldExists(LOG_FIELD_NAME, typeNode) != MemberExistsResult.NOT_EXISTS) {
            annotationNode.addWarning("Field '" + LOG_FIELD_NAME + "' already exists.");
            return false;
        }
        return true;
    }

    private JCTree.JCVariableDecl createField(final AnnotationValues<SpongeLog> annotation, final JavacNode annotationNode, final JavacNode typeNode) {
        JavacTreeMaker maker = typeNode.getTreeMaker();
        Name name = ((JCTree.JCClassDecl) typeNode.get()).name;
        JCTree.JCFieldAccess loggingType = maker.Select(maker.Ident(name), typeNode.toName("class"));
        JCTree.JCExpression loggerType = chainDotsString(typeNode, "org.slf4j.Logger");
        JCTree.JCExpression factoryMethod = chainDotsString(typeNode, "org.slf4j.LoggerFactory.getLogger");

        JCTree.JCExpression loggerName;
        String topic = annotation.getInstance().topic();
        if (topic == null || topic.trim().length() == 0) { // 1
            loggerName = loggingType;
        } else {
            loggerName = maker.Literal(topic);
        }

        JCTree.JCMethodInvocation factoryMethodCall = maker.Apply(List.<JCTree.JCExpression>nil(), factoryMethod, loggerName != null ? List.of(loggerName) : List.<JCTree.JCExpression>nil());

        return recursiveSetGeneratedBy(maker.VarDef(
                maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC ),
                typeNode.toName(LOG_FIELD_NAME), loggerType, factoryMethodCall)
                , annotationNode.get(), typeNode.getContext());
    }


}
