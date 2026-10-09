package com.josebieco.dirtcheapgolems.client;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Shared model of every cheap golem: a small, squat golem with a head, body, two thick arms and short legs.
 * Texture UV layout is documented in tools/gen_textures.py.
 */
public class CheapGolemModel extends EntityModel<CheapGolemRenderState> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(Identifier.fromNamespaceAndPath(DirtCheapGolems.MOD_ID, "cheap_golem"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public CheapGolemModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Ground is y = 24. Legs 20..24, body 14..20, head 9..14 (≈ 0.94 blocks tall).
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -5.0F, -2.5F, 6.0F, 5.0F, 5.0F),
            PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 6.0F, 5.0F),
            PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F),
            PartPose.offset(-5.5F, 14.5F, 0.0F));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(32, 16).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 8.0F, 3.0F),
            PartPose.offset(5.5F, 14.5F, 0.0F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offset(-2.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(48, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
            PartPose.offset(2.0F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CheapGolemRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(pos, 13.0F)) * speed;
        this.leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(pos, 13.0F)) * speed;
        this.rightLeg.xRot = -1.5F * Mth.triangleWave(pos, 13.0F) * speed;
        this.leftLeg.xRot = 1.5F * Mth.triangleWave(pos, 13.0F) * speed;
    }
}
