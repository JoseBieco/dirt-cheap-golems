package com.josebieco.dirtcheapgolems.client;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.entity.CrackStage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Renderer shared by every cheap golem; only the texture changes. Adds crack overlays by {@link CrackStage}. */
public class CheapGolemRenderer<T extends AbstractCheapGolem> extends MobRenderer<T, CheapGolemRenderState, CheapGolemModel> {
    private final Identifier texture;
    private boolean bobbing;

    public CheapGolemRenderer(EntityRendererProvider.Context context, Identifier texture) {
        super(context, new CheapGolemModel(context.bakeLayer(CheapGolemModel.LAYER)), 0.4F);
        this.texture = texture;
        this.addLayer(new CrackLayer(this));
    }

    /** Adds a full-bright layer (eyes and glowing cracks), used by the Netherrack Golem. */
    public void addEmissiveLayer(Identifier glowTexture) {
        var renderType = RenderTypes.eyes(glowTexture);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return renderType;
            }
        });
    }

    /** Makes the golem float up and down (client only), used by the End Stone Golem. */
    public void enableBobbing() {
        this.bobbing = true;
    }

    @Override
    protected void setupRotations(CheapGolemRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (bobbing) poseStack.translate(0.0F, Math.sin(state.ageInTicks * 0.1F) * 0.1F, 0.0F);
    }

    public static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(DirtCheapGolems.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public Identifier getTextureLocation(CheapGolemRenderState state) {
        return texture;
    }

    @Override
    public CheapGolemRenderState createRenderState() {
        return new CheapGolemRenderState();
    }

    @Override
    public void extractRenderState(T entity, CheapGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.crackStage = entity.getCrackStage();
    }

    static class CrackLayer extends RenderLayer<CheapGolemRenderState, CheapGolemModel> {
        private static final Identifier CRACKS_LOW = texture("cracks_low");
        private static final Identifier CRACKS_HIGH = texture("cracks_high");

        CrackLayer(RenderLayerParent<CheapGolemRenderState, CheapGolemModel> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                           CheapGolemRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.crackStage == CrackStage.NONE) return;
            var cracks = state.crackStage == CrackStage.LOW ? CRACKS_LOW : CRACKS_HIGH;
            renderColoredCutoutModel(this.getParentModel(), cracks, poseStack, collector, lightCoords, state, -1, 1);
        }
    }
}
