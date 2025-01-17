package com.TominoCZ.FBP.particle;

import com.TominoCZ.FBP.FBP;
import com.TominoCZ.FBP.renderer.FBPRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleSmokeNormal;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.List;

public class FBPParticleSmokeNormal extends ParticleSmokeNormal {

	Minecraft mc;

	double startScale, scaleAlpha, prevParticleScale, prevParticleAlpha;
	double endMult = 0.75;

	Vec3d[] cube;

	Vec2f par;

	ParticleSmokeNormal original;

	protected FBPParticleSmokeNormal(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, final double mX, final double mY, final double mZ, float scale, TextureAtlasSprite tex, ParticleSmokeNormal original) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, mX, mY, mZ, scale);

		this.original = original;

		this.motionX = mX;
		this.motionY = mY;
		this.motionZ = mZ;

		mc = Minecraft.getMinecraft();
		this.particleTexture = tex;

		scaleAlpha = particleScale * 0.85;

		Block block = worldIn.getBlockState(new BlockPos(xCoordIn, yCoordIn, zCoordIn)).getBlock();

		if (block == Blocks.FIRE) {
			this.particleScale *= 0.65f;
			this.particleGravity *= 0.25f;

			this.motionX = FBP.random.nextDouble(-0.05, 0.05);
			this.motionY = FBP.random.nextDouble() * 0.5;
			this.motionZ = FBP.random.nextDouble(-0.05, 0.05);

			this.motionY *= 0.35f;

			scaleAlpha = particleScale * 0.5;

			particleMaxAge = FBP.random.nextInt(7, 18);
		} else if (block == Blocks.TORCH) {
			particleScale *= 0.45f;

			this.motionX = FBP.random.nextDouble(-0.05, 0.05);
			this.motionY = FBP.random.nextDouble() * 0.5;
			this.motionZ = FBP.random.nextDouble(-0.05, 0.05);

			this.motionX *= 0.925f;
			this.motionY = 0.005f;
			this.motionZ *= 0.925f;

			this.particleRed = 0.275f;
			this.particleGreen = 0.275f;
			this.particleBlue = 0.275f;

			scaleAlpha = particleScale * 0.75;

			particleMaxAge = FBP.random.nextInt(5, 10);
		} else {
			particleScale = scale;
			motionY *= 0.935;
		}

		particleScale *= FBP.scaleMult;

		startScale = particleScale;

		float angleY = rand.nextFloat() * 80;

		cube = new Vec3d[FBP.CUBE.length];

		for (int i = 0; i < FBP.CUBE.length; i++) {
			Vec3d vec = FBP.CUBE[i];
			cube[i] = FBPRenderer.rotatef_d(vec, 0, angleY, 0);
		}

		particleAlpha = 1f;

		if (FBP.randomFadingSpeed)
			endMult = MathHelper.clamp(FBP.random.nextDouble(0.425, 1.15), 0.5432, 1);

		multipleParticleScaleBy(1);
	}

	@Override
	public Particle multipleParticleScaleBy(float scale) {
		Particle p = super.multipleParticleScaleBy(scale);

		float f = particleScale / 20;

		this.setBoundingBox(new AxisAlignedBB(posX - f, posY - f, posZ - f, posX + f, posY + f, posZ + f));

		return p;
	}

	@Override
	public int getFXLayer()
	{
		return 1;
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;

		prevParticleAlpha = particleAlpha;
		prevParticleScale = particleScale;

		if (!FBP.fancySmoke)
			this.isExpired = true;

		if (++this.particleAge >= this.particleMaxAge) {
			if (FBP.randomFadingSpeed)
				particleScale *= 0.887654321F * endMult;
			else
				particleScale *= 0.887654321F;

			if (particleAlpha > 0.01 && particleScale <= scaleAlpha) {
				if (FBP.randomFadingSpeed)
					particleAlpha *= 0.7654321F * endMult;
				else
					particleAlpha *= 0.7654321F;
			}

			if (particleAlpha <= 0.01)
				setExpired();
		}

		this.motionY += 0.004D;
		this.move(this.motionX, this.motionY, this.motionZ);

		if (this.posY == this.prevPosY) {
			this.motionX *= 1.1D;
			this.motionZ *= 1.1D;
		}

		this.motionX *= 0.9599999785423279D;
		this.motionY *= 0.9599999785423279D;
		this.motionZ *= 0.9599999785423279D;

		if (this.onGround) {
			this.motionX *= 0.899999988079071D;
			this.motionZ *= 0.899999988079071D;
		}
	}

	@Override
	public void move(double x, double y, double z) {
		double Y = y;

		List<AxisAlignedBB> list = this.world.getCollisionBoxes(null, this.getBoundingBox().expand(x, y, z));

		for (AxisAlignedBB axisalignedbb : list) {
			y = axisalignedbb.calculateYOffset(this.getBoundingBox(), y);
		}

		this.setBoundingBox(this.getBoundingBox().offset(0.0D, y, 0.0D));

		for (AxisAlignedBB axisalignedbb : list) {
			x = axisalignedbb.calculateXOffset(this.getBoundingBox(), x);
		}

		this.setBoundingBox(this.getBoundingBox().offset(x, 0.0D, 0.0D));

		for (AxisAlignedBB axisalignedbb : list) {
			z = axisalignedbb.calculateZOffset(this.getBoundingBox(), z);
		}

		this.setBoundingBox(this.getBoundingBox().offset(0.0D, 0.0D, z));

		// RESET
		resetPositionToBB();
		this.onGround = y != Y;
	}

	@Override
	protected void resetPositionToBB() {
		AxisAlignedBB axisalignedbb = this.getBoundingBox();
		this.posX = (axisalignedbb.minX + axisalignedbb.maxX) / 2.0D;
		this.posY = (axisalignedbb.minY + axisalignedbb.maxY) / 2.0D;
		this.posZ = (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D;
	}

	@Override
	public void renderParticle(BufferBuilder worldRendererIn, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (!FBP.isEnabled() && particleMaxAge != 0)
			particleMaxAge = 0;

		float f = particleTexture.getInterpolatedU((0.1f + 1) / 4 * 16);
		float f1 = particleTexture.getInterpolatedV((0.1f + 1) / 4 * 16);

		float f5 = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpPosX);
		float f6 = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpPosY);
		float f7 = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ);

		int i = getBrightnessForRender(partialTicks);

		float alpha = (float) (prevParticleAlpha + (particleAlpha - prevParticleAlpha) * partialTicks);

		// SMOOTH TRANSITION
		float f4 = (float) (prevParticleScale + (particleScale - prevParticleScale) * partialTicks);

		// RENDER
		par = new Vec2f(f, f1);

		worldRendererIn.setTranslation(f5, f6, f7);
		putCube(worldRendererIn, f4 / 20, i >> 16 & 65535, i & 65535, particleRed, particleGreen, particleBlue, alpha);
		worldRendererIn.setTranslation(0, 0, 0);
	}

	public void putCube(BufferBuilder worldRendererIn, double scale, int j, int k, float r, float g, float b, float a) {
		float brightnessForRender = 1;

		float R;
		float G;
		float B;

		for (int i = 0; i < cube.length; i += 4) {
			Vec3d v1 = cube[i];
			Vec3d v2 = cube[i + 1];
			Vec3d v3 = cube[i + 2];
			Vec3d v4 = cube[i + 3];

			R = r * brightnessForRender;
			G = g * brightnessForRender;
			B = b * brightnessForRender;

			brightnessForRender *= 0.875;

			addVt(worldRendererIn, scale, v1, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v2, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v3, par.x, par.y, j, k, R, G, B, a);
			addVt(worldRendererIn, scale, v4, par.x, par.y, j, k, R, G, B, a);
		}
	}

	private void addVt(BufferBuilder worldRendererIn, double scale, Vec3d pos, double u, double v, int j, int k, float r, float g, float b, float a) { // add vertex to buffer
		worldRendererIn.pos(pos.x * scale, pos.y * scale, pos.z * scale).tex(u, v).color(r, g, b, a).lightmap(j, k)
				.endVertex();
	}

	@Override
	public int getBrightnessForRender(float p_189214_1_) {
		int i = super.getBrightnessForRender(p_189214_1_);
		int j = 0;

		if (this.world.isBlockLoaded(new BlockPos(posX, posY, posZ)))
		{
			j = this.world.getCombinedLight(new BlockPos(posX, posY, posZ), 0);
		}

		return i == 0 ? j : i;
	}

	@Override
	public void setExpired() {
		this.isExpired = true;

		original.setExpired();
	}
}
