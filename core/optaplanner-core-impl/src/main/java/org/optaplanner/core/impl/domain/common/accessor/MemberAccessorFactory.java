/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.optaplanner.core.api.domain.common.DomainAccessType;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.domain.common.ReflectionHelper;
import org.optaplanner.core.impl.domain.common.accessor.gizmo.GizmoMemberAccessorFactory;

public class MemberAccessorFactory {

    // exists only so that the various member accessors can share the same text in their exception messages
    static final String CLASSLOADER_NUDGE_MESSAGE = "Maybe add getClass().getClassLoader() as a parameter to the " +
            SolverFactory.class.getSimpleName() + ".create...() method call.";

    /**
     * As defined by {@link #buildMemberAccessor(Member, MemberAccessorType, Class, DomainAccessType, Map)},
     * but caches the result in the map if provided.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType
     * @param annotationClass the annotation the member was annotated with (used for error reporting)
     * @param domainAccessType
     * @param memberAccessorMap key is the fully qualified member name
     * @return never null, new {@link MemberAccessor} instance unless already found in memberAccessorMap
     */
    public static MemberAccessor buildMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass, DomainAccessType domainAccessType,
            Map<String, MemberAccessor> memberAccessorMap) {
        if (memberAccessorMap == null) {
            return buildMemberAccessor(member, memberAccessorType, annotationClass, domainAccessType);
        }
        String generatedClassName = GizmoMemberAccessorFactory.getGeneratedClassName(member);
        return memberAccessorMap.computeIfAbsent(generatedClassName,
                k -> buildMemberAccessor(member, memberAccessorType, annotationClass, domainAccessType));
    }

    /**
     * Creates a new member accessor based on the given parameters.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType
     * @param annotationClass the annotation the member was annotated with (used for error reporting)
     * @param domainAccessType
     * @return never null, new instance of the member accessor
     */
    public static MemberAccessor buildMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass, DomainAccessType domainAccessType) {
        switch (domainAccessType) {
            case GIZMO:
                return GizmoMemberAccessorFactory.buildGizmoMemberAccessor(member, annotationClass);
            case REFLECTION:
                return buildReflectiveMemberAccessor(member, memberAccessorType, annotationClass);
            default:
                throw new IllegalStateException("The domainAccessType (" + domainAccessType + ") is not implemented.");
        }
    }

    private static MemberAccessor buildReflectiveMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass) {
        if (member instanceof Field) {
            Field field = (Field) member;
            return new ReflectionFieldMemberAccessor(field);
        } else if (member instanceof Method) {
            Method method = (Method) member;
            MemberAccessor memberAccessor;
            switch (memberAccessorType) {
                case FIELD_OR_READ_METHOD:
                    if (!ReflectionHelper.isGetterMethod(method)) {
                        ReflectionHelper.assertReadMethod(method, annotationClass);
                        memberAccessor = new ReflectionMethodMemberAccessor(method);
                        break;
                    }
                    // Intentionally fall through (no break)
                case FIELD_OR_GETTER_METHOD:
                case FIELD_OR_GETTER_METHOD_WITH_SETTER:
                    boolean getterOnly = memberAccessorType != MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;
                    ReflectionHelper.assertGetterMethod(method, annotationClass);
                    if (Modifier.isPublic(method.getModifiers())
                            // HACK The lambda approach doesn't support classes from another classloader (such as loaded by KieContainer) in JDK 8
                            // TODO In JDK 9 use MethodHandles.privateLookupIn(Class, MethodHandles.lookup())
                            && method.getDeclaringClass().getClassLoader().equals(MemberAccessor.class.getClassLoader())) {
                        memberAccessor = new LambdaBeanPropertyMemberAccessor(method, getterOnly);
                    } else {
                        memberAccessor = new ReflectionBeanPropertyMemberAccessor(method, getterOnly);
                    }
                    break;
                default:
                    throw new IllegalStateException("The memberAccessorType (" + memberAccessorType
                            + ") is not implemented.");
            }
            if (memberAccessorType == MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER
                    && !memberAccessor.supportSetter()) {
                throw new IllegalStateException("The class (" + method.getDeclaringClass()
                        + ") has a @" + annotationClass.getSimpleName()
                        + " annotated getter method (" + method
                        + "), but lacks a setter for that property (" + memberAccessor.getName() + ").");
            }
            return memberAccessor;
        } else {
            throw new IllegalStateException("Impossible state: the member (" + member + ")'s type is not a "
                    + Field.class.getSimpleName() + " or a " + Method.class.getSimpleName() + ".");
        }
    }

    public enum MemberAccessorType {
        FIELD_OR_READ_METHOD,
        FIELD_OR_GETTER_METHOD,
        FIELD_OR_GETTER_METHOD_WITH_SETTER
    }

    private MemberAccessorFactory() {
    }

}
