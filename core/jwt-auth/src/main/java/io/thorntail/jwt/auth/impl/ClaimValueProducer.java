/**
 *
 *   Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.thorntail.jwt.auth.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;

/**
 * A producer for the ClaimValue<T> wrapper injection sites.
 * @param <T> the raw claim type
 */
@Dependent
public class ClaimValueProducer<T> {

    @Produces
    @Claim("")
    ClaimValue<T> produce(InjectionPoint ip) {
        String name = getName(ip);
        ClaimValue<Optional<T>> cv = JsonWebTokenProducer.generalClaimValueProducer(name);
        ClaimValue<T> returnValue = (ClaimValue<T>) cv;
        Optional<T> value = cv.getValue();
        // Pull out the ClaimValue<T> T type,
        Type matchType = ip.getType();
        Type actualType = Object.class;
        boolean isOptional = false;
        if (matchType instanceof ParameterizedType) {
            actualType = ((ParameterizedType) matchType).getActualTypeArguments()[0];
            isOptional = matchType.getTypeName().equals(Optional.class.getTypeName());
            if (isOptional) {
                actualType = ((ParameterizedType) matchType).getActualTypeArguments()[0];
            }
        }

        if (!actualType.getTypeName().startsWith(Optional.class.getTypeName())) {
            T nestedValue = value.orElse(null);
            ClaimValueWrapper<T> wrapper = new ClaimValueWrapper<>(cv.getName());
            wrapper.setValue(nestedValue);
            returnValue = wrapper;
        }
        return returnValue;
    }

    String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }
}

