package net.zepalesque.redux.client.render.entity.model.entity;

import com.aetherteam.aether_genesis.entity.monster.BattleSentry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.zepalesque.redux.capability.animation.sentry.battle.BattleSentryAnimation;
import net.zepalesque.redux.config.ReduxConfig;
import net.zepalesque.redux.util.math.EasingUtil;
import net.zepalesque.redux.util.math.MathUtil;

public class BattleSentryReduxModel<T extends BattleSentry> extends EntityModel<T> {
	private final ModelPart body;
	private final ModelPart legFL;
	private final ModelPart legFR;
	private final ModelPart legBL;
	private final ModelPart legBR;

	public BattleSentryReduxModel(ModelPart root) {
		this.body = root.getChild("body");
		this.legFL = this.body.getChild("legFL");
		this.legFR = this.body.getChild("legFR");
		this.legBL = this.body.getChild("legBL");
		this.legBR = this.body.getChild("legBR");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -4.0F, -6.5F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.5F));

		PartDefinition saws = body.addOrReplaceChild("saws", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition legFL = body.addOrReplaceChild("legFL", CubeListBuilder.create(), PartPose.offset(5.0F, 8.0F, -6.5F));

		PartDefinition cube_r1 = legFL.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(22, 26).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition legFR = body.addOrReplaceChild("legFR", CubeListBuilder.create(), PartPose.offset(-5.0F, 8.0F, -6.5F));

		PartDefinition cube_r2 = legFR.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(22, 26).mirror().addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition legBL = body.addOrReplaceChild("legBL", CubeListBuilder.create(), PartPose.offset(5.0F, 8.0F, 5.5F));

		PartDefinition cube_r3 = legBL.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(22, 26).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

		PartDefinition legBR = body.addOrReplaceChild("legBR", CubeListBuilder.create(), PartPose.offset(-5.0F, 8.0F, 5.5F));

		PartDefinition cube_r4 = legBR.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(22, 26).mirror().addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

		BattleSentryAnimation.get(entity).ifPresent((anim) -> {

			int wake = anim.getWakeAnim();
			int prevWake = anim.getPrevWakeAnim();
			float trueWake = Mth.lerp(ageInTicks % 1, prevWake, wake);
			float progressWake = (5 - trueWake) / 5;

			this.body.y = 14.0F + (EasingUtil.Back.inOut(progressWake)) * 2F;


			int jump = anim.getJumpAnim();
			int prevJump = jump > 1 && anim.getPrevJumpAnim() == 0 ? 10 : anim.getPrevJumpAnim();
			float trueJump = Mth.lerp(ageInTicks % 1, prevJump, jump);
			float progress = (10 - trueJump) / 10;

			if (progress <= 0.3F)
			{
				this.legFR.xRot = MathUtil.costrp(1F - progressWake, MathUtil.degToRad(135F), EasingUtil.Cubic.inOut(progress / 0.3F) * MathUtil.degToRad(75F));
				this.legFL.xRot = this.legFR.xRot;
				this.legBR.xRot = MathUtil.costrp(1F - progressWake, MathUtil.degToRad(-135F),EasingUtil.Cubic.inOut(progress / 0.3F) * MathUtil.degToRad(10F));
				this.legBL.xRot = this.legBR.xRot;
			} else {
				this.legFR.xRot = MathUtil.costrp(1F - progressWake, MathUtil.degToRad(135F),MathUtil.degToRad(75F) - EasingUtil.Cubic.inOut((progress - 0.3F) / 0.7F) * MathUtil.degToRad(75F));
				this.legFL.xRot = this.legFR.xRot;
				this.legBR.xRot = MathUtil.costrp(1F - progressWake, MathUtil.degToRad(-135F),MathUtil.degToRad(10F) - EasingUtil.Cubic.inOut((progress - 0.3F) / 0.7F) * MathUtil.degToRad(10F));
				this.legBL.xRot = this.legBR.xRot;
			}



		});


	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

		if (ReduxConfig.CLIENT.sentry_improvements.get()) {
			body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		}
	}
}