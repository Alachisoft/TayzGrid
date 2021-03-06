// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Value.proto

package com.alachisoft.tayzgrid.common.protobuf;

public final class ValueProtocol {
  private ValueProtocol() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class Value extends
      com.google.protobuf.GeneratedMessage {
    // Use Value.newBuilder() to construct.
    private Value() {
      initFields();
    }
    private Value(boolean noInit) {}
    
    private static final Value defaultInstance;
    public static Value getDefaultInstance() {
      return defaultInstance;
    }
    
    public Value getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_fieldAccessorTable;
    }
    
    // repeated bytes data = 1;
    public static final int DATA_FIELD_NUMBER = 1;
    private java.util.List<com.google.protobuf.ByteString> data_ =
      java.util.Collections.emptyList();
    public java.util.List<com.google.protobuf.ByteString> getDataList() {
      return data_;
    }
    public int getDataCount() { return data_.size(); }
    public com.google.protobuf.ByteString getData(int index) {
      return data_.get(index);
    }
    
    private void initFields() {
    }
    public final boolean isInitialized() {
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      for (com.google.protobuf.ByteString element : getDataList()) {
        output.writeBytes(1, element);
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      {
        int dataSize = 0;
        for (com.google.protobuf.ByteString element : getDataList()) {
          dataSize += com.google.protobuf.CodedOutputStream
            .computeBytesSizeNoTag(element);
        }
        size += dataSize;
        size += 1 * getDataList().size();
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value result;
      
      // Construct using com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value();
        return builder;
      }
      
      protected com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.getDescriptor();
      }
      
      public com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value getDefaultInstanceForType() {
        return com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        if (result.data_ != java.util.Collections.EMPTY_LIST) {
          result.data_ =
            java.util.Collections.unmodifiableList(result.data_);
        }
        com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value) {
          return mergeFrom((com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value other) {
        if (other == com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.getDefaultInstance()) return this;
        if (!other.data_.isEmpty()) {
          if (result.data_.isEmpty()) {
            result.data_ = new java.util.ArrayList<com.google.protobuf.ByteString>();
          }
          result.data_.addAll(other.data_);
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 10: {
              addData(input.readBytes());
              break;
            }
          }
        }
      }
      
      
      // repeated bytes data = 1;
      public java.util.List<com.google.protobuf.ByteString> getDataList() {
        return java.util.Collections.unmodifiableList(result.data_);
      }
      public int getDataCount() {
        return result.getDataCount();
      }
      public com.google.protobuf.ByteString getData(int index) {
        return result.getData(index);
      }
      public Builder setData(int index, com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.data_.set(index, value);
        return this;
      }
      public Builder addData(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  if (result.data_.isEmpty()) {
          result.data_ = new java.util.ArrayList<com.google.protobuf.ByteString>();
        }
        result.data_.add(value);
        return this;
      }
      public Builder addAllData(
          java.lang.Iterable<? extends com.google.protobuf.ByteString> values) {
        if (result.data_.isEmpty()) {
          result.data_ = new java.util.ArrayList<com.google.protobuf.ByteString>();
        }
        super.addAll(values, result.data_);
        return this;
      }
      public Builder clearData() {
        result.data_ = java.util.Collections.emptyList();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:com.alachisoft.tayzgrid.common.protobuf.Value)
    }
    
    static {
      defaultInstance = new Value(true);
      com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:com.alachisoft.tayzgrid.common.protobuf.Value)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\013Value.proto\022\'com.alachisoft.tayzgrid.c" +
      "ommon.protobuf\"\025\n\005Value\022\014\n\004data\030\001 \003(\014B\017B" +
      "\rValueProtocol"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_com_alachisoft_tayzgrid_common_protobuf_Value_descriptor,
              new java.lang.String[] { "Data", },
              com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.class,
              com.alachisoft.tayzgrid.common.protobuf.ValueProtocol.Value.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}
