/*******************************************************************************
 * Copyright (c) 2016 Francisco Gonzalez-Armijo Riádigos
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
 ******************************************************************************/

package com.kuassivi.examples.repository_cache.dummy;

import com.kuassivi.annotation.RepositoryCache;

import android.text.format.DateUtils;

public interface MyRepository {

    /**
     * Calls on this method will be cached indefinitely, unless you evict it later.
     *
     * @return any retrieved data
     */
    @RepositoryCache
    Object[] getAllDataOnce();

    /**
     * Calls on this method with a specific id will be cached until 30 minutes each one,
     * then expires.
     *
     * @param id any Id
     * @return any retrieved data
     */
    @RepositoryCache(DateUtils.MINUTE_IN_MILLIS * 30)
    Object getDataById(int id);
}
