/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.Activity.defaultopts;

import org.opendatakit.tables.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class DefaultsActivity extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.defaultoptions_view);
		TabHost th = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		// the addition defaults
		intent = new Intent().setClass(this, AdditionDefaults.class);
		spec = th.newTabSpec("adds");
		spec.setIndicator("Addition");
		spec.setContent(intent);
		th.addTab(spec);
		// the query defaults
		intent = new Intent().setClass(this, QueryDefaults.class);
		spec = th.newTabSpec("queries");
		spec.setIndicator("Querying");
		spec.setContent(intent);
		th.addTab(spec);
	}
	
}
