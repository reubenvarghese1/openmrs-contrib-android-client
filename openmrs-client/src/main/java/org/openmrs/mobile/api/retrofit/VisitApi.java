/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.mobile.api.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.openmrs.mobile.api.RestApi;
import org.openmrs.mobile.api.RestServiceBuilder;
import org.openmrs.mobile.application.OpenMRS;
import org.openmrs.mobile.dao.EncounterDAO;
import org.openmrs.mobile.dao.LocationDAO;
import org.openmrs.mobile.dao.VisitDAO;
import org.openmrs.mobile.listeners.retrofit.DefaultResponseCallbackListener;
import org.openmrs.mobile.listeners.retrofit.GetVisitTypeCallbackListener;
import org.openmrs.mobile.listeners.retrofit.StartVisitResponseListenerCallback;
import org.openmrs.mobile.models.Encounter;
import org.openmrs.mobile.models.Patient;
import org.openmrs.mobile.models.Results;
import org.openmrs.mobile.models.Visit;
import org.openmrs.mobile.models.VisitType;
import org.openmrs.mobile.utilities.ApplicationConstants;
import org.openmrs.mobile.utilities.DateUtils;
import org.openmrs.mobile.utilities.ToastUtil;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisitApi {

    public void syncVisitsData(@NonNull Patient patient) {
        syncVisitsData(patient, null);
    }

    public void syncVisitsData(@NonNull final Patient patient, @Nullable final DefaultResponseCallbackListener callbackListener) {

        RestApi restApi = RestServiceBuilder.createService(RestApi.class);
        Call<Results<Visit>> call = restApi.findVisitsByPatientUUID(patient.getUuid(), "custom:(uuid,location:ref,visitType:ref,startDatetime,stopDatetime,encounters:full)");
        call.enqueue(new Callback<Results<Visit>>() {
            @Override
            public void onResponse(Call<Results<Visit>> call, Response<Results<Visit>> response) {
                if (response.isSuccessful()) {
                    List<Visit> visits = response.body().getResults();
                    VisitDAO visitDAO = new VisitDAO();
                    for (Visit visit : visits) {
                        long visitId = visitDAO.getVisitsIDByUUID(visit.getUuid());

                        if (visitId > 0) {
                            visitDAO.updateVisit(visit, visitId, patient.getId());
                        } else  {
                            visitDAO.saveVisit(visit, patient.getId());
                        }
                    }
                    if (callbackListener != null) {
                        callbackListener.onResponse();
                    }
                }
                else {
                    ToastUtil.error(response.message());
                    if (callbackListener != null) {
                        callbackListener.onErrorResponse();
                    }
                }
            }

            @Override
            public void onFailure(Call<Results<Visit>> call, Throwable t) {
                ToastUtil.error(t.getMessage());
                if (callbackListener != null) {
                    callbackListener.onErrorResponse();
                }
            }

        });
    }

    public void getVisitType(final GetVisitTypeCallbackListener callbackListener) {
        RestApi restApi = RestServiceBuilder.createService(RestApi.class);
        Call<Results<VisitType>> call = restApi.getVisitType();
        call.enqueue(new Callback<Results<VisitType>>() {

            @Override
            public void onResponse(Call<Results<VisitType>> call, Response<Results<VisitType>> response) {
                if (response.isSuccessful()) {
                    callbackListener.onGetVisitTypeResponse(response.body().getResults().get(0));
                }
                else {
                    callbackListener.onErrorResponse();
                }
            }

            @Override
            public void onFailure(Call<Results<VisitType>> call, Throwable t) {
                callbackListener.onErrorResponse();
            }

        });
    }

    public void syncLastVitals(final String patientUuid) {
        syncLastVitals(patientUuid, null);
    }
    public void syncLastVitals(final String patientUuid, @Nullable final DefaultResponseCallbackListener callbackListener) {
        RestApi restApi = RestServiceBuilder.createService(RestApi.class);
        Call<Results<Encounter>> call = restApi.getLastVitals(patientUuid, ApplicationConstants.EncounterTypes.VITALS, "full", 1,"desc");
        call.enqueue(new Callback<Results<Encounter>>() {
            @Override
            public void onResponse(Call<Results<Encounter>> call, Response<Results<Encounter>> response) {
                if (response.isSuccessful()) {
                    if (!response.body().getResults().isEmpty()) {
                        new EncounterDAO().saveLastVitalsEncounter(response.body().getResults().get(0), patientUuid);
                    }
                    if (callbackListener != null) {
                        callbackListener.onResponse();
                    }
                }
                else {
                    if (callbackListener != null) {
                        callbackListener.onErrorResponse();
                    }
                }
            }

            @Override
            public void onFailure(Call<Results<Encounter>> call, Throwable t) {
                if (callbackListener != null) {
                    callbackListener.onErrorResponse();
                }
            }
        });
    }


    public void startVisit(final Patient patient) {
        startVisit(patient, null);
    }

    public void startVisit(final Patient patient, @Nullable final StartVisitResponseListenerCallback callbackListener) {

        RestApi restApi = RestServiceBuilder.createService(RestApi.class);
        final Visit visit = new Visit();
        visit.setStartDatetime(DateUtils.convertTime(System.currentTimeMillis(), DateUtils.OPEN_MRS_REQUEST_FORMAT));
        visit.setPatient(patient);
        visit.setLocation(LocationDAO.findLocationByName(OpenMRS.getInstance().getLocation()));
        visit.setVisitType(new VisitType(null, OpenMRS.getInstance().getVisitTypeUUID()));

        Call<Visit> call = restApi.startVisit(visit);
        call.enqueue(new Callback<Visit>() {
            @Override
            public void onResponse(Call<Visit> call, Response<Visit> response) {
                if (response.isSuccessful()) {
                    long id = new VisitDAO().saveVisit(response.body(), patient.getId());
                    if(callbackListener != null) {
                        callbackListener.onStartVisitResponse(id);
                    }
                }
                else {
                    ToastUtil.error(response.message());
                    if(callbackListener != null) {
                        callbackListener.onErrorResponse();
                    }
                }
            }

            @Override
            public void onFailure(Call<Visit> call, Throwable t) {
                if(callbackListener != null) {
                    callbackListener.onErrorResponse();
                }
                else {
                    ToastUtil.error(t.getMessage());

                }
            }
        });
    }

}
