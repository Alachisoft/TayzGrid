// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: GetRunningTasksResponse.proto

package com.alachisoft.tayzgrid.common.protobuf;

public final class GetRunningTasksResponseProtocol {
  private GetRunningTasksResponseProtocol() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class GetRunningTasksResponse extends
      com.google.protobuf.GeneratedMessage {
    // Use GetRunningTasksResponse.newBuilder() to construct.
    private GetRunningTasksResponse() {
      initFields();
    }
    private GetRunningTasksResponse(boolean noInit) {}
    
    private static final GetRunningTasksResponse defaultInstance;
    public static GetRunningTasksResponse getDefaultInstance() {
      return defaultInstance;
    }
    
    public GetRunningTasksResponse getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_fieldAccessorTable;
    }
    
    // repeated string runningTasks = 1;
    public static final int RUNNINGTASKS_FIELD_NUMBER = 1;
    private java.util.List<java.lang.String> runningTasks_ =
      java.util.Collections.emptyList();
    public java.util.List<java.lang.String> getRunningTasksList() {
      return runningTasks_;
    }
    public int getRunningTasksCount() { return runningTasks_.size(); }
    public java.lang.String getRunningTasks(int index) {
      return runningTasks_.get(index);
    }
    
    private void initFields() {
    }
    public final boolean isInitialized() {
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      for (java.lang.String element : getRunningTasksList()) {
        output.writeString(1, element);
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
        for (java.lang.String element : getRunningTasksList()) {
          dataSize += com.google.protobuf.CodedOutputStream
            .computeStringSizeNoTag(element);
        }
        size += dataSize;
        size += 1 * getRunningTasksList().size();
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseDelimitedFrom(
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
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse result;
      
      // Construct using com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse();
        return builder;
      }
      
      protected com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.getDescriptor();
      }
      
      public com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse getDefaultInstanceForType() {
        return com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        if (result.runningTasks_ != java.util.Collections.EMPTY_LIST) {
          result.runningTasks_ =
            java.util.Collections.unmodifiableList(result.runningTasks_);
        }
        com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse) {
          return mergeFrom((com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse other) {
        if (other == com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.getDefaultInstance()) return this;
        if (!other.runningTasks_.isEmpty()) {
          if (result.runningTasks_.isEmpty()) {
            result.runningTasks_ = new java.util.ArrayList<java.lang.String>();
          }
          result.runningTasks_.addAll(other.runningTasks_);
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
              addRunningTasks(input.readString());
              break;
            }
          }
        }
      }
      
      
      // repeated string runningTasks = 1;
      public java.util.List<java.lang.String> getRunningTasksList() {
        return java.util.Collections.unmodifiableList(result.runningTasks_);
      }
      public int getRunningTasksCount() {
        return result.getRunningTasksCount();
      }
      public java.lang.String getRunningTasks(int index) {
        return result.getRunningTasks(index);
      }
      public Builder setRunningTasks(int index, java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.runningTasks_.set(index, value);
        return this;
      }
      public Builder addRunningTasks(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  if (result.runningTasks_.isEmpty()) {
          result.runningTasks_ = new java.util.ArrayList<java.lang.String>();
        }
        result.runningTasks_.add(value);
        return this;
      }
      public Builder addAllRunningTasks(
          java.lang.Iterable<? extends java.lang.String> values) {
        if (result.runningTasks_.isEmpty()) {
          result.runningTasks_ = new java.util.ArrayList<java.lang.String>();
        }
        super.addAll(values, result.runningTasks_);
        return this;
      }
      public Builder clearRunningTasks() {
        result.runningTasks_ = java.util.Collections.emptyList();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponse)
    }
    
    static {
      defaultInstance = new GetRunningTasksResponse(true);
      com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponse)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\035GetRunningTasksResponse.proto\022\'com.ala" +
      "chisoft.tayzgrid.common.protobuf\"/\n\027GetR" +
      "unningTasksResponse\022\024\n\014runningTasks\030\001 \003(" +
      "\tB!B\037GetRunningTasksResponseProtocol"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_com_alachisoft_tayzgrid_common_protobuf_GetRunningTasksResponse_descriptor,
              new java.lang.String[] { "RunningTasks", },
              com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.class,
              com.alachisoft.tayzgrid.common.protobuf.GetRunningTasksResponseProtocol.GetRunningTasksResponse.Builder.class);
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