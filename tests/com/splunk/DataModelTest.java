/*
 * Copyright 2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.splunk;

import com.sun.accessibility.internal.resources.accessibility_en;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class DataModelTest extends SDKTestCase {
    @After
    public void tearDown() throws Exception {
        for (DataModel d : service.getDataModels().values()) {
            if (d.getName().startsWith("delete-me")) {
                d.remove();
            }
        }

        super.tearDown();
    }

    @Test
    public void testDataModelCollectionCreateAndDelete() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/empty_data_model.json")));

        int initialN = dataModels.size();

        DataModel dataModel = dataModels.create(createTemporaryName(), args);
        Assert.assertEquals(initialN+1, dataModels.size());

        dataModel.remove();
        dataModels.refresh();
        Assert.assertEquals(initialN, dataModels.size());
    }

    @Test
    public void testDataModelWithZeroObjects() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/empty_data_model.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        Assert.assertEquals(0, model.getObjects().size());

        // Make sure this works after refresh as well.
        model.refresh();
        Assert.assertEquals(0, model.getObjects().size());
    }

    @Test
    public void testDataModelWithOneObject() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/object_with_one_search.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        Assert.assertEquals(1, model.getObjects().size());

        // Make sure this works after refresh as well.
        model.refresh();
        Assert.assertEquals(1, model.getObjects().size());
    }

    @Test
    public void testDataModelWithTwoObjects() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/object_with_two_searches.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        Assert.assertEquals(2, model.getObjects().size());

        // Make sure this works after refresh as well.
        model.refresh();
        Assert.assertEquals(2, model.getObjects().size());
    }

    @Test
    public void testGetAndContainsObjectWork() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/object_with_two_searches.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        Assert.assertTrue(model.containsObject("search1"));
        Assert.assertTrue(model.containsObject("search2"));

        DataModelObject search1 = model.getObject("search1");
        Assert.assertNotNull(search1);
        Assert.assertEquals("\u0bf5\u0bf1\u0bf0\u0bef - search 1", search1.getDisplayName());

        DataModelObject search2 = model.getObject("search2");
        Assert.assertNotNull(search2);
        Assert.assertEquals("\u0bf5\u0bf1\u0bf0\u0bef - search 2", search2.getDisplayName());
    }

    @Test
    public void testDataModelWithUnicodeAttributes() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        String modelName = createTemporaryName();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/model_with_unicode_headers.json")));
        DataModel model = dataModels.create(modelName, args);

        Assert.assertEquals(modelName, model.getName());
        Assert.assertEquals("\u1029\u1699\u0bf5", model.getDisplayName());
        Assert.assertEquals("\u0bf5\u0bf1\u0bf0\u0bef", model.getDescription());
    }

    @Test
    public void testDataModelWithEmptyAttributes() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        String modelName = createTemporaryName();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/model_with_empty_headers.json")));
        DataModel model = dataModels.create(modelName, args);

        Assert.assertEquals(modelName, model.getName());
        Assert.assertEquals("", model.getDisplayName());
        Assert.assertEquals("", model.getDescription());
    }

    @Test
    public void testAccelerationSettings() {
        Args serviceArgs = new Args();
        serviceArgs.put("host", service.getHost());
        serviceArgs.put("port", service.getPort());
        serviceArgs.put("scheme", service.getScheme());
        serviceArgs.put("token", service.getToken());

        serviceArgs.put("owner", "nobody");
        serviceArgs.put("app", "search");
        Service nonprivateService = new Service(serviceArgs);

        EntityCollection<DataModel> dataModels = nonprivateService.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        model.setAcceleration(true);
        model.setEarliestAcceleratedTime("-2mon");
        model.setAccelerationCronSchedule("5/* * * * *");

        Assert.assertTrue(model.isAccelerated());
        Assert.assertEquals("-2mon", model.getEarliestAcceleratedTime());
        Assert.assertEquals("5/* * * * *", model.getAccelerationCronSchedule());

        model.update();
        model.refresh();

        Assert.assertTrue(model.isAccelerated());
        Assert.assertEquals("-2mon", model.getEarliestAcceleratedTime());
        Assert.assertEquals("5/* * * * *", model.getAccelerationCronSchedule());

        model.setAcceleration(false);
        model.setEarliestAcceleratedTime("-1mon");
        model.setAccelerationCronSchedule("* * * * *");

        Assert.assertFalse(model.isAccelerated());
        Assert.assertEquals("-1mon", model.getEarliestAcceleratedTime());
        Assert.assertEquals("* * * * *", model.getAccelerationCronSchedule());
    }

    @Test
    public void testObjectMetadata() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = models.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");
        Assert.assertNotNull(object);

        Assert.assertEquals("event1 \u1029\u1699", object.getDisplayName());
        Assert.assertEquals("\u1029\u1699\u0bf5 comment on event1", object.getComment());
        Assert.assertEquals("event1", object.getName());
        Assert.assertEquals(model, object.getDataModel());
    }

    @Test
    public void testLineage() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(createTemporaryName(), args);

        Collection<DataModelObject> children;

        DataModelObject object = model.getObject("level_0");
        Assert.assertNotNull(object);
        Assert.assertArrayEquals(new String[]{"level_0"}, object.getLineage());
        Assert.assertEquals("BaseEvent", object.getParentName());
        Assert.assertArrayEquals(new String[]{"level_1"}, object.getChildrenNames().toArray());
        children = object.getChildren();
        Assert.assertEquals(1, children.size());
        boolean more_than_one = false;
        for (DataModelObject child : children) {
            Assert.assertFalse(more_than_one);
            Assert.assertEquals("level_1", child.getName());
            more_than_one = true;
        }


        object = model.getObject("level_1");
        Assert.assertNotNull(object);
        Assert.assertArrayEquals(new String[]{"level_0", "level_1"}, object.getLineage());
        Assert.assertEquals("level_0", object.getParentName());
        Assert.assertArrayEquals(new String[] {"level_2"}, object.getChildrenNames().toArray());

        object = model.getObject("level_2");
        Assert.assertNotNull(object);
        Assert.assertArrayEquals(new String[]{"level_0", "level_1", "level_2"}, object.getLineage());
        Assert.assertEquals("level_1", object.getParentName());
        Assert.assertArrayEquals(new String[] {}, object.getChildrenNames().toArray());
        children = object.getChildren();
        Assert.assertEquals(0, children.size());
    }

    @Test
    public void testObjectFields() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("level_2");
        Assert.assertNotNull(object);
        Assert.assertEquals(5, object.getAutoExtractedFields().size());

        Field f = object.getField("_time");
        Assert.assertEquals("BaseEvent", f.getOwnerName());
        Assert.assertArrayEquals(new String[]{"BaseEvent"}, f.getOwnerLineage());
        Assert.assertEquals(FieldType.TIMESTAMP, f.getType());
        Assert.assertEquals("_time", f.getName());
        Assert.assertEquals("_time", f.getDisplayName());
        Assert.assertEquals(false, f.isRequired());
        Assert.assertEquals(false, f.isMultivalued());
        Assert.assertEquals(false, f.isHidden());
        Assert.assertEquals(false, f.isEditable());
        Assert.assertEquals("", f.getComment());

        f = object.getField("level_2");
        Assert.assertEquals("level_2", f.getOwnerName());
        Assert.assertArrayEquals(new String[]{"level_0", "level_1", "level_2"}, f.getOwnerLineage());
        Assert.assertEquals(FieldType.OBJECTCOUNT, f.getType());
        Assert.assertEquals("level_2", f.getName());
        Assert.assertEquals("level 2", f.getDisplayName());
        Assert.assertEquals(false, f.isRequired());
        Assert.assertEquals(false, f.isMultivalued());
        Assert.assertEquals(false, f.isHidden());
        Assert.assertEquals(false, f.isEditable());
        Assert.assertEquals("", f.getComment());
    }

    @Test
    public void testOutputObjectFields() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_for_pivot.json")));
        DataModel model = models.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("test_data");
        Assert.assertEquals(5, object.getAutoExtractedFields().size());
        Assert.assertEquals(10, object.getAllFields().size());
        Assert.assertTrue(object.containsField("has_boris"));
        Assert.assertTrue(object.containsField("_time"));
    }


    @Test
    public void testCreateLocalAccelerationJob() {
        DataModelCollection models = service.getDataModels();

        String dataModelName = createTemporaryName();
        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(dataModelName, args);

        DataModelObject object = model.getObject("level_2");
        Assert.assertNotNull(object);

        final Job accelerationJob = object.createLocalAccelerationJob();

        try {
            assertEventuallyTrue(new EventuallyTrueBehavior() {
                @Override
                public boolean predicate() {
                    return accelerationJob.isReady();
                }
            });

            Assert.assertEquals(
                    "| datamodel " + dataModelName + " level_2 search | tscollect",
                    accelerationJob.getSearch()
            );
        } finally {
            accelerationJob.cancel();
        }
    }

    @Test
    public void testCreateLocalAccelerationJobWithEarliestTime() {
        DataModelCollection models = service.getDataModels();

        String dataModelName = createTemporaryName();
        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(dataModelName, args);

        DataModelObject object = model.getObject("level_2");
        Assert.assertNotNull(object);

        final Job accelerationJob = object.createLocalAccelerationJob("-1d");
        try {
            assertEventuallyTrue(new EventuallyTrueBehavior() {
                @Override
                public boolean predicate() {
                    return accelerationJob.isReady();
                }
            });

            Assert.assertEquals(
                    "| datamodel " + dataModelName + " level_2 search | tscollect",
                    accelerationJob.getSearch()
            );
        } finally {
            accelerationJob.cancel();
        }
        // I don't have a good way of getting a date two weeks ago in Java, so
        // the earliest time part of this is simply untested.
    }

    @Test
    public void testAcceleratedQueryEqualsQueryOnUnacceleratedModel() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("level_2");

        Assert.assertEquals(object.getQuery(), object.getAcceleratedQuery());
    }

    @Test
    public void testAcceleratedQueryWithNamespace() {
        DataModelCollection models = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/inheritance_test_data.json")));
        DataModel model = models.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("level_2");

        final Job accelerationJob = object.createLocalAccelerationJob();

        try {
            assertEventuallyTrue(new EventuallyTrueBehavior() {
                @Override
                public boolean predicate() {
                    return accelerationJob.isReady();
                }
            });

            Job job = service.getJobs().create("tstats count from sid=" + accelerationJob.getSid());
            job.cancel();
        } finally {
            accelerationJob.cancel();
        }

        Assert.assertEquals(object.getQuery(), object.getAcceleratedQuery());
    }

    @Test
    public void testAcceleratedQueryOnAcceleratedModelWorks() {
        Args serviceArgs = new Args();
        serviceArgs.put("host", service.getHost());
        serviceArgs.put("port", service.getPort());
        serviceArgs.put("scheme", service.getScheme());
        serviceArgs.put("token", service.getToken());

        serviceArgs.put("owner", "nobody");
        serviceArgs.put("app", "search");
        Service nonprivateService = new Service(serviceArgs);

        EntityCollection<DataModel> dataModels = nonprivateService.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        model.setAcceleration(true);

        DataModelObject object = model.getObject("event1");

        Assert.assertTrue(object.getAcceleratedQuery().startsWith("| tstats"));

        final Job job = nonprivateService.getJobs().create(object.getAcceleratedQuery());

        assertEventuallyTrue(new EventuallyTrueBehavior() {
            @Override
            public boolean predicate() {
                return job.isReady();
            }
        });

        Assert.assertEquals(0, job.getEventCount());
    }

    @Test
    public void testAcceleratedQueryWorksOnUnacceleratedModel() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");

        Assert.assertTrue(object.getAcceleratedQuery().startsWith("| datamodel"));

        final Job job = service.getJobs().create(object.getAcceleratedQuery());

        assertEventuallyTrue(new EventuallyTrueBehavior() {
            @Override
            public boolean predicate() {
                return job.isReady();
            }
        });

        Assert.assertEquals(0, job.getEventCount());
    }

    @Test
    public void testQueryWorksOnModel() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");

        Assert.assertTrue(object.getQuery().startsWith("| datamodel"));

        final Job job = service.getJobs().create(object.getQuery());

        assertEventuallyTrue(new EventuallyTrueBehavior() {
            @Override
            public boolean predicate() {
                return job.isReady();
            }
        });

        Assert.assertEquals(0, job.getEventCount());
    }

    @Test
    public void testConstraints() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");
        Assert.assertNotNull(object);

        Assert.assertEquals(1, object.getConstraints().size());
        boolean only_one = true;
        for (Constraint c : object.getConstraints()) {
            Assert.assertTrue(only_one);
            only_one = false;
            Assert.assertEquals("event1", c.getOwner());
            Assert.assertEquals("uri=\"*.php\" OR uri=\"*.py\"\n" +
                    "NOT (referer=null OR referer=\"-\")", c.getQuery());
        }
    }

    @Test
    public void testCalculations() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");
        Assert.assertNotNull(object);

        Map<String, Calculation> calculations = object.getCalculations();
        Assert.assertEquals(4, calculations.size());

        EvalCalculation c = (EvalCalculation)calculations.get("93fzsv03wa7");
        Assert.assertEquals("event1", c.getOwner());
        Assert.assertArrayEquals(new String[]{"event1"}, c.getLineage());
        Assert.assertEquals("", c.getComment());
        Assert.assertEquals(true, c.isEditable());
        Assert.assertEquals(
                "if(cidrmatch(\"192.0.0.0/16\", clientip), \"local\", \"other\")",
                c.getExpression()
        );
        Assert.assertEquals(1, c.getGeneratedFields().size());
        Field f = c.getGeneratedField("new_field");
        Assert.assertNotNull(f);
        Assert.assertEquals("My New Field", f.getDisplayName());

        LookupCalculation lc = (LookupCalculation)calculations.get("sr3mc8o3mjr");
        Assert.assertEquals("event1", lc.getOwner());
        Assert.assertArrayEquals(new String[]{"event1"}, lc.getLineage());
        Assert.assertEquals("", lc.getComment());
        Assert.assertEquals(true, lc.isEditable());
        Assert.assertEquals("host", lc.getInputField());
        Assert.assertEquals("dnslookup", lc.getLookupName());
        Assert.assertEquals("a_lookup_field", lc.getLookupFieldName());

        RegexpCalculation rc = (RegexpCalculation)calculations.get("a5v1k82ymic");
        Assert.assertEquals(2, rc.getGeneratedFields().size());
        Assert.assertEquals("_raw", rc.getInputField());
        Assert.assertEquals(" From: (?<from>.*) To: (?<to>.*) ", rc.getExpression());

        GeoIPCalculation gc = (GeoIPCalculation)calculations.get("pbe9bd0rp4");
        Assert.assertEquals("\u1029\u1699\u0bf5 comment of pbe9bd0rp4", gc.getComment());
        Assert.assertEquals(5, gc.getGeneratedFields().size());
        Assert.assertEquals("output_from_reverse_hostname", gc.getInputField());
    }

    @Test
    public void testRunQuery() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/data_model_with_test_objects.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("event1");

        Job job = null;
        try {
            final Job j = object.runQuery();
            job = j;
            assertEventuallyTrue(new EventuallyTrueBehavior() {
                @Override
                public boolean predicate() {
                    return j.isReady();
                }
            });
            Assert.assertEquals(object.getAcceleratedQuery(), job.getSearch());
        } finally {
            if (job != null) {
                job.cancel();
            }
        }

        job = null;
        try {
            job = object.runQuery("| head 3", new JobArgs() {{
                setEnableLookups(false);
                put("status_buckets", "5");
            }});
            final Job j = job;
            assertEventuallyTrue(new EventuallyTrueBehavior() {
                @Override
                public boolean predicate() {
                    return j.isReady();
                }
            });
            Assert.assertEquals(object.getAcceleratedQuery() + "| head 3", job.getSearch());
            Assert.assertEquals(5, job.getInteger("statusBuckets"));
        } finally {
            if (job != null) {
                job.cancel();
            }
        }
    }

    @Test
    public void testBaseSearchProperlyParsed() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/model_with_multiple_types.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("search1");
        Assert.assertNotNull(object);
        Assert.assertTrue(object instanceof DataModelSearch);
        DataModelSearch s = (DataModelSearch)object;

        Assert.assertEquals("BaseSearch", s.getParentName());
        Assert.assertEquals("search index=_internal | head 10", s.getBaseSearch());
    }

    @Test
    public void testBaseTransactionProperlyParsed() {
        EntityCollection<DataModel> dataModels = service.getDataModels();

        DataModelArgs args = new DataModelArgs();
        args.setRawDescription(streamToString(openResource("data/datamodels/model_with_multiple_types.json")));
        DataModel model = dataModels.create(createTemporaryName(), args);

        DataModelObject object = model.getObject("transaction1");
        Assert.assertNotNull(object);
        Assert.assertTrue(object instanceof DataModelTransaction);
        DataModelTransaction t = (DataModelTransaction)object;

        Assert.assertArrayEquals(new String[] {"event1"}, t.getObjectsToGroup().toArray());
        Assert.assertArrayEquals(new String[] {"host", "from"}, t.getGroupByFields().toArray());

        Assert.assertEquals("25s", t.getMaxPause());
        Assert.assertEquals("100m", t.getMaxSpan());
    }
}
