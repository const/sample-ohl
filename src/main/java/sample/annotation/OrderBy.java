package sample.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface OrderBy {
    Value[] value();

    @Target({})
    @interface Value {
        String value();

        boolean desc() default false;
    }
}
