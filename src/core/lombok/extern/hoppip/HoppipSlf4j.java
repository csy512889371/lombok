package lombok.extern.hoppip;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @className HoppipSlf4j
 * @author: liuzhen
 * @create: 2019-12-23 22:08
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface HoppipSlf4j {
    String[] names() default {};
    String[] params() default {};
}
