package com.google.typography.font.sfntly.table.opentype.component;

import com.google.typography.font.sfntly.data.ReadableFontData;
import com.google.typography.font.sfntly.data.WritableFontData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class RecordList<T extends Record> implements Iterable<T> {
  private static final int COUNT_OFFSET = 0;
  public static final int RECORD_BASE_DEFAULT = 2;
  public final int base;
  public final int recordBase;

  final ReadableFontData readData;
  private final WritableFontData writeData;
  private int count;
  private List<T> recordsToWrite;

  /*
   *public RecordList(WritableFontData data) { this.readData = null;
   * this.writeData = data; this.count = 0; this.base = 0; this.recordBase =
   * RECORD_BASE_DEFAULT; if (writeData != null) {
   * writeData.writeUShort(COUNT_OFFSET, 0); } }
   */
  public RecordList(ReadableFontData data, int base, int recordBaseOffset, int countDecrement) {
    this.readData = data;
    this.writeData = null;
    this.base = base;
    this.recordBase = base + RECORD_BASE_DEFAULT + recordBaseOffset;
    if (readData != null) {
      this.count = data.readUShort(base + COUNT_OFFSET) - countDecrement;
    }
  }

  public RecordList(ReadableFontData data) {
    this(data, 0);
  }

  public RecordList(ReadableFontData data, int countDecrement) {
    this(data, 0, 0, countDecrement);
  }

  public int count() {
    if (recordsToWrite != null) {
      return recordsToWrite.size();
    }
    return count;
  }

  public int limit() {
    return sizeOfList(count());
  }

  private int sizeOfList(int count) {
    return baseAt(recordBase, count);
  }

  private int baseAt(int base, int index) {
    return base + index * recordSize();
  }

  public T get(int index) {
    if (recordsToWrite != null) {
      return recordsToWrite.get(index);
    }
    return getRecordAt(readData, sizeOfList(index));
  }

  public boolean contains(T record) {
    if (recordsToWrite != null) {
      return recordsToWrite.contains(record);
    }

    Iterator<T> iterator = iterator();
    while (iterator.hasNext()) {
      if (record.equals(iterator.next())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Iterator<T> iterator() {
    if (recordsToWrite != null) {
      return recordsToWrite.iterator();
    }

    return new Iterator<T>() {
      private int current = 0;

      @Override
      public boolean hasNext() {
        return current < count;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return getRecordAt(readData, sizeOfList(current++));
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public RecordList<T> add(T record) {
    copyFromRead();
    recordsToWrite.add(record);
    return this;
  }

  public RecordList<T> addAll(Collection<T> recordsToWrite) {
    copyFromRead();
    this.recordsToWrite.addAll(recordsToWrite);
    return this;
  }

  public int write() {
    if (writeData == null) {
      throw new UnsupportedOperationException();
    }
    return writeTo(writeData);
  }

  public int writeTo(WritableFontData writeData) {
    copyFromRead();

    writeData.writeUShort(base + COUNT_OFFSET, count);
    int nextWritePos = recordBase;
    for (T record : recordsToWrite) {
      nextWritePos += record.writeTo(writeData, nextWritePos);
    }
    return nextWritePos - recordBase + RECORD_BASE_DEFAULT; // bytes wrote
  }

  private void copyFromRead() {
    if (recordsToWrite == null) {
      recordsToWrite = new ArrayList<T>(count);
      Iterator<T> iterator = iterator();
      while (iterator.hasNext()) {
        recordsToWrite.add(iterator.next());
      }
    }
  }

  protected abstract T getRecordAt(ReadableFontData data, int pos);

  protected abstract int recordSize();
}