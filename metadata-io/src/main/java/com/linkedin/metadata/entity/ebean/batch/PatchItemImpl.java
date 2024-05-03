package com.linkedin.metadata.entity.ebean.batch;

import static com.linkedin.metadata.Constants.INGESTION_MAX_SERIALIZED_STRING_LENGTH;
import static com.linkedin.metadata.Constants.MAX_JACKSON_STRING_SIZE;
import static com.linkedin.metadata.entity.AspectUtils.validateAspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.events.metadata.ChangeType;
import com.linkedin.metadata.aspect.AspectRetriever;
import com.linkedin.metadata.aspect.batch.MCPItem;
import com.linkedin.metadata.aspect.batch.PatchMCP;
import com.linkedin.metadata.aspect.patch.template.AspectTemplateEngine;
import com.linkedin.metadata.entity.validation.ValidationUtils;
import com.linkedin.metadata.models.AspectSpec;
import com.linkedin.metadata.models.EntitySpec;
import com.linkedin.metadata.models.registry.EntityRegistry;
import com.linkedin.metadata.utils.EntityKeyUtils;
import com.linkedin.metadata.utils.SystemMetadataUtils;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.mxe.SystemMetadata;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Builder(toBuilder = true)
public class PatchItemImpl implements PatchMCP {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    int maxSize =
        Integer.parseInt(
            System.getenv()
                .getOrDefault(INGESTION_MAX_SERIALIZED_STRING_LENGTH, MAX_JACKSON_STRING_SIZE));
    OBJECT_MAPPER
        .getFactory()
        .setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(maxSize).build());
  }

  // urn an urn associated with the new aspect
  private final Urn urn;
  // aspectName name of the aspect being inserted
  private final String aspectName;
  private final SystemMetadata systemMetadata;
  private final AuditStamp auditStamp;

  private final Patch patch;

  private final MetadataChangeProposal metadataChangeProposal;

  // derived
  private final EntitySpec entitySpec;
  private final AspectSpec aspectSpec;

  @Nonnull
  @Override
  public ChangeType getChangeType() {
    return ChangeType.PATCH;
  }

  @Nullable
  @Override
  public RecordTemplate getRecordTemplate() {
    return null;
  }

  public ChangeItemImpl applyPatch(RecordTemplate recordTemplate, AspectRetriever aspectRetriever) {
    ChangeItemImpl.ChangeItemImplBuilder builder =
        ChangeItemImpl.builder()
            .urn(getUrn())
            .aspectName(getAspectName())
            .metadataChangeProposal(getMetadataChangeProposal())
            .auditStamp(auditStamp)
            .systemMetadata(getSystemMetadata());

    AspectTemplateEngine aspectTemplateEngine =
        aspectRetriever.getEntityRegistry().getAspectTemplateEngine();

    RecordTemplate currentValue =
        recordTemplate != null
            ? recordTemplate
            : aspectTemplateEngine.getDefaultTemplate(getAspectName());

    if (currentValue == null) {
      // Attempting to patch a value to an aspect which has no default value and no existing value.
      throw new UnsupportedOperationException(
          String.format(
              "Patch not supported for aspect with name %s. "
                  + "Default aspect is required because no aspect currently exists for urn %s.",
              getAspectName(), getUrn()));
    }

    try {
      builder.recordTemplate(
          aspectTemplateEngine.applyPatch(currentValue, getPatch(), getAspectSpec()));
    } catch (JsonProcessingException | JsonPatchException e) {
      throw new RuntimeException(e);
    }

    return builder.build(aspectRetriever);
  }

  public static class PatchItemImplBuilder {

    public PatchItemImpl.PatchItemImplBuilder systemMetadata(SystemMetadata systemMetadata) {
      this.systemMetadata = SystemMetadataUtils.generateSystemMetadataIfEmpty(systemMetadata);
      return this;
    }

    public PatchItemImpl build(EntityRegistry entityRegistry) {
      ValidationUtils.validateUrn(entityRegistry, this.urn);
      log.debug("entity type = {}", this.urn.getEntityType());

      entitySpec(entityRegistry.getEntitySpec(this.urn.getEntityType()));
      log.debug("entity spec = {}", this.entitySpec);

      aspectSpec(ValidationUtils.validate(this.entitySpec, this.aspectName));
      log.debug("aspect spec = {}", this.aspectSpec);

      if (this.patch == null) {
        throw new IllegalArgumentException(
            String.format("Missing patch to apply. Aspect: %s", this.aspectSpec.getName()));
      }

      return new PatchItemImpl(
          this.urn,
          this.aspectName,
          SystemMetadataUtils.generateSystemMetadataIfEmpty(this.systemMetadata),
          this.auditStamp,
          this.patch,
          this.metadataChangeProposal,
          this.entitySpec,
          this.aspectSpec);
    }

    public static PatchItemImpl build(
        MetadataChangeProposal mcp, AuditStamp auditStamp, EntityRegistry entityRegistry) {
      log.debug("entity type = {}", mcp.getEntityType());
      EntitySpec entitySpec = entityRegistry.getEntitySpec(mcp.getEntityType());
      AspectSpec aspectSpec = validateAspect(mcp, entitySpec);

      if (!MCPItem.isValidChangeType(ChangeType.PATCH, aspectSpec)) {
        throw new UnsupportedOperationException(
            "ChangeType not supported: "
                + mcp.getChangeType()
                + " for aspect "
                + mcp.getAspectName());
      }

      Urn urn = mcp.getEntityUrn();
      if (urn == null) {
        urn = EntityKeyUtils.getUrnFromProposal(mcp, entitySpec.getKeyAspectSpec());
      }

      return PatchItemImpl.builder()
          .urn(urn)
          .aspectName(mcp.getAspectName())
          .systemMetadata(
              SystemMetadataUtils.generateSystemMetadataIfEmpty(mcp.getSystemMetadata()))
          .metadataChangeProposal(mcp)
          .auditStamp(auditStamp)
          .patch(convertToJsonPatch(mcp))
          .build(entityRegistry);
    }

    private static Patch convertToJsonPatch(MetadataChangeProposal mcp) {
      JsonNode json;
      try {
        json = OBJECT_MAPPER.readTree(mcp.getAspect().getValue().asString(StandardCharsets.UTF_8));
        return JsonPatch.fromJson(json);
      } catch (IOException e) {
        throw new IllegalArgumentException("Invalid JSON Patch: " + mcp.getAspect().getValue(), e);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PatchItemImpl that = (PatchItemImpl) o;
    return urn.equals(that.urn)
        && aspectName.equals(that.aspectName)
        && Objects.equals(systemMetadata, that.systemMetadata)
        && patch.equals(that.patch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(urn, aspectName, systemMetadata, patch);
  }

  @Override
  public String toString() {
    return "PatchBatchItem{"
        + "urn="
        + urn
        + ", aspectName='"
        + aspectName
        + '\''
        + ", systemMetadata="
        + systemMetadata
        + ", patch="
        + patch
        + '}';
  }
}
