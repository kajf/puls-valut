/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License
 */

package ch.prokopovi.ui.main;

import android.database.AbstractCursor;
import android.database.Cursor;

/**
 * Cursor wrapper that reorders rows according to supplied specific position
 * mapping.
 */
public class ReorderingCursorWrapper extends AbstractCursor {
	private final Cursor mCursor;
	private final int[] mPositionMap;

	/**
	 * Constructor.
	 * 
	 * @param cursor
	 *            wrapped cursor
	 * @param positionMap
	 *            maps wrapper cursor positions to wrapped cursor positions so
	 *            that positionMap[wrapperPosition] == wrappedPosition
	 */
	public ReorderingCursorWrapper(Cursor cursor, int[] positionMap) {
		if (cursor.getCount() != positionMap.length) {
			throw new IllegalArgumentException(
					"Cursor and position map have different sizes.");
		}

		this.mCursor = cursor;
		this.mPositionMap = positionMap;
	}

	@Override
	public void close() {
		super.close();
		this.mCursor.close();
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition) {
		return this.mCursor.moveToPosition(this.mPositionMap[newPosition]);
	}

	@Override
	public String[] getColumnNames() {
		return this.mCursor.getColumnNames();
	}

	@Override
	public int getCount() {
		return this.mCursor.getCount();
	}

	@Override
	public double getDouble(int column) {
		return this.mCursor.getDouble(column);
	}

	@Override
	public float getFloat(int column) {
		return this.mCursor.getFloat(column);
	}

	@Override
	public int getInt(int column) {
		return this.mCursor.getInt(column);
	}

	@Override
	public long getLong(int column) {
		return this.mCursor.getLong(column);
	}

	@Override
	public short getShort(int column) {
		return this.mCursor.getShort(column);
	}

	@Override
	public String getString(int column) {
		return this.mCursor.getString(column);
	}

	@Override
	public boolean isNull(int column) {
		return this.mCursor.isNull(column);
	}
}
