package com.TominoCZ.FBP.particle;

import com.TominoCZ.FBP.FBP;
import com.TominoCZ.FBP.keys.FBPKeyBindings;
import com.TominoCZ.FBP.renderer.FBPRenderer;
import com.TominoCZ.FBP.util.FBPMathUtil;
import com.TominoCZ.FBP.vector.FBPVector3d;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

@SideOnly(Side.CLIENT)
public class FBPParticleDigging extends ParticleDigging {

	private final IBlockState sourceState;

	Minecraft mc;

	float prevGravity;

	double startY, scaleAlpha, prevParticleScale, prevParticleAlpha, prevMotionX, prevMotionZ;
	double endMult = 0.75;

	boolean modeDebounce, wasFrozen, destroyed, killToggle;

	EnumFacing facing;

	FBPVector3d rot, prevRot, rotStep;

	Vec2f[] par;

	static Entity dummyEntity = new Entity(null) {
		@Override
		protected void writeEntityToNBT(NBTTagCompound compound) {
			// TODO Auto-generated method stub
		}

		@Override
		protected void readEntityFromNBT(NBTTagCompound compound) {
			// TODO Auto-generated method stub
		}

		@Override
		protected void entityInit() {
			// TODO Auto-generated method stub
		}
	};

	protected FBPParticleDigging(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, float scale, float R, float G, float B, IBlockState state, @Nullable EnumFacing facing, @Nullable TextureAtlasSprite texture) {
		super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, state);

		this.particleRed = R;
		this.particleGreen = G;
		this.particleBlue = B;

		mc = Minecraft.getMinecraft();

		rot = new FBPVector3d();
		prevRot = new FBPVector3d();

		this.facing = facing;

		createRotationMatrix();

		this.sourcePos = new BlockPos(xCoordIn, yCoordIn, zCoordIn);

		if (scale > -1)
			particleScale = scale;

		if (scale < -1) {
			if (facing != null) {
				if (facing == EnumFacing.UP && FBP.smartBreaking) {
					motionX *= 1.5D;
					motionY *= 0.1D;
					motionZ *= 1.5D;

					double particleSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);

					double x = FBPMathUtil.add(cameraViewDir.x, 0.01D);
					double z = FBPMathUtil.add(cameraViewDir.z, 0.01D);

					motionX = x * particleSpeed;
					motionZ = z * particleSpeed;
				}
			}
		}

		if (modeDebounce == !FBP.randomRotation) {
			this.rot.zero();
			calculateYAngle();
		}

		this.sourceState = state;

		Block b = state.getBlock();

		particleGravity = (float) (b.blockParticleGravity * FBP.gravityMult);

		particleScale = (float) (FBP.scaleMult * (FBP.randomizedScale ? particleScale : 1));
		particleMaxAge = (int) FBP.random.nextDouble(FBP.minAge, FBP.maxAge + 0.5);

		scaleAlpha = particleScale * 0.82;

		destroyed = facing == null;

		if (texture == null) {
			BlockModelShapes blockModelShapes = mc.getBlockRendererDispatcher().getBlockModelShapes();

			// GET THE TEXTURE OF THE BLOCK FACE
			if (!destroyed) {
				try {
					List<BakedQuad> quads = blockModelShapes.getModelForState(state).getQuads(state, facing, 0);

					if (!quads.isEmpty())
						this.particleTexture = quads.get(0).getSprite();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			if (particleTexture == null || particleTexture.getIconName().equals("missingno"))
				this.setParticleTexture(blockModelShapes.getTexture(state));
		} else
			this.particleTexture = texture;

		if (FBP.randomFadingSpeed)
			endMult = MathHelper.clamp(FBP.random.nextDouble(0.5, 0.9), 0.55, 0.8);

		prevGravity = particleGravity;

		startY = posY;

		multipleParticleScaleBy(1);
	}

	@Override
	public Particle multipleParticleScaleBy(float scale) {
		Particle p = super.multipleParticleScaleBy(scale);

		float f = particleScale / 10;

		if (FBP.restOnFloor && destroyed)
			posY = prevPosY = startY - f;

		this.setBoundingBox(new AxisAlignedBB(posX - f, posY, posZ - f, posX + f, posY + 2 * f, posZ + f));

		return p;
	}

	public Particle MultiplyVelocity(float multiplier) {
		this.motionX *= multiplier;
		this.motionY = (this.motionY - 0.10000000149011612D) * (multiplier / 2) + 0.10000000149011612D;
		this.motionZ *= multiplier;
		return this;
	}

	@Override
	protected void multiplyColor(@Nullable BlockPos p_187154_1_) {
		if (sourceState.getBlock() == Blocks.GRASS && facing != EnumFacing.UP)
			return;

		int i = mc.getBlockColors().colorMultiplier(this.sourceState, this.world, p_187154_1_, 0);
		this.particleRed *= (i >> 16 & 255) / 255.0F;
		this.particleGreen *= (i >> 8 & 255) / 255.0F;
		this.particleBlue *= (i & 255) / 255.0F;
	}

	@Override
	public FBPParticleDigging init() {
		multiplyColor(new BlockPos(this.posX, this.posY, this.posZ));
		return this;
	}

	@Override
	public FBPParticleDigging setBlockPos(BlockPos pos) {
		this.multiplyColor(pos);
		return this;
	}

	@Override
	public int getFXLayer() {
		return 1;
	}

	@Override
	public void onUpdate() {
		boolean allowedToMove = MathHelper.abs((float) motionX) > 0.0001f || MathHelper.abs((float) motionZ) > 0.0001f;

		if (!FBP.frozen && FBP.bounceOffWalls && !mc.isGamePaused() && particleAge > 0) {
			if (!wasFrozen && allowedToMove) {
				boolean xCollided = prevPosX == posX;
				boolean zCollided = prevPosZ == posZ;

				if (xCollided)
					motionX = -prevMotionX * 0.625f;
				if (zCollided)
					motionZ = -prevMotionZ * 0.625f;

				if (!FBP.randomRotation && (xCollided || zCollided))
					calculateYAngle();
			} else
				wasFrozen = false;
		}
		if (FBP.frozen && FBP.bounceOffWalls && !wasFrozen)
			wasFrozen = true;

		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;

		prevRot.copyFrom(rot);

		prevParticleAlpha = particleAlpha;
		prevParticleScale = particleScale;

		if (!mc.isGamePaused() && (!FBP.frozen || killToggle)) {
			if (!killToggle) {
				if (!FBP.randomRotation) {
					if (!modeDebounce) {
						modeDebounce = true;

						rot.z = 0;

						calculateYAngle();
					}

					if (allowedToMove) {
						float x = MathHelper.abs((float) (rotStep.x * getMult()));

						if (motionX > 0) {
							if (motionZ > 0)
								rot.x -= x;
							else if (motionZ < 0)
								rot.x += x;
						} else if (motionX < 0) {
							if (motionZ < 0)
								rot.x += x;
							else if (motionZ > 0)
							{
								rot.x -= x;
							}
						}
					}
				} else {
					if (modeDebounce)
					{
						modeDebounce = false;

						rot.z = FBP.random.nextDouble(30, 400);
					}

					if (allowedToMove)
						rot.add(rotStep.multiply(getMult()));
				}
			}

			if (!FBP.infiniteDuration)
				particleAge++;

			if (this.particleAge >= this.particleMaxAge || killToggle) {
				particleScale *= 0.88f * endMult;

				if (particleAlpha > 0.01 && particleScale <= scaleAlpha)
					particleAlpha *= 0.68f * endMult;

				if (particleAlpha <= 0.01)
					setExpired();
			}

			if (!killToggle) {
				if (!onGround)
					motionY -= 0.04f * particleGravity;

				move(motionX, motionY, motionZ);

				if (onGround && FBP.restOnFloor) {
					rot.x = (float) Math.round(rot.x / 90) * 90;
					rot.z = (float) Math.round(rot.z / 90) * 90;
				}

				if (MathHelper.abs((float) motionX) > 0.00001f)
					prevMotionX = motionX;
				if (MathHelper.abs((float) motionZ) > 0.00001f)
					prevMotionZ = motionZ;

				if (allowedToMove) {
					motionX *= 0.98f;
					motionZ *= 0.98f;
				}

				motionY *= 0.98f;

				// PHYSICS
				if (FBP.entityCollision) {
					List<Entity> list = world.getEntitiesWithinAABB(Entity.class, this.getBoundingBox());

					for (Entity entityIn : list) {
						if (!entityIn.noClip) {
							float f0 = (float) (this.posX - entityIn.posX);
							float f1 = (float) (this.posZ - entityIn.posZ);
							float f2 = (float) MathHelper.absMax(f0, f1);

							if (f2 >= 0.0099f) {
								f2 = (float) Math.sqrt(f2);
								f0 /= f2;
								f1 /= f2;

								float f3 = 1.0f / f2;

								if (f3 > 1.0f)
									f3 = 1.0f;

								this.motionX += f0 * f3 / 20;
								this.motionZ += f1 * f3 / 20;

								if (!FBP.randomRotation)
									calculateYAngle();
								if (!FBP.frozen)
									this.onGround = false;
							}
						}
					}
				}

				if (FBP.waterPhysics) {
					if (isInWater()) {
						handleWaterMovement();

						if (FBP.INSTANCE.doesMaterialFloat(this.sourceState.getMaterial())) {
							motionY = 0.11f + (particleScale / 1.25f) * 0.02f;
						} else {
							motionX *= 0.93f;
							motionZ *= 0.93f;
							particleGravity = 0.35f;

							motionY *= 0.85f;
						}

						if (!FBP.randomRotation)
							calculateYAngle();

						if (onGround)
							onGround = false;
					} else {
						particleGravity = prevGravity;
					}
				}

				if (onGround) {
					if (FBP.lowTraction) {
						motionX *= 0.93f;
						motionZ *= 0.93f;
					} else {
						motionX *= 0.66f;
						motionZ *= 0.66f;
					}
				}
			}
		}
	}

	public boolean isInWater() {
		double scale = particleScale / 20;

		int minX = MathHelper.floor(posX - scale);
		int maxX = MathHelper.ceil(posX + scale);

		int minY = MathHelper.floor(posY - scale);
		int maxY = MathHelper.ceil(posY + scale);

		int minZ = MathHelper.floor(posZ - scale);
		int maxZ = MathHelper.ceil(posZ + scale);

		if (world.isAreaLoaded(new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), true)) {
			for (int x = minX; x < maxX; ++x) {
				for (int y = minY; y < maxY; ++y) {
					for (int z = minZ; z < maxZ; ++z) {
						IBlockState block = world.getBlockState(new BlockPos(x, y, z));

						if (block.getMaterial() == Material.WATER) {
							double d0 = (float) (y + 1) - BlockLiquid.getLiquidHeightPercent(block.getValue(BlockLiquid.LEVEL));

							if (posY <= d0)
								return true;
						}
					}
				}
			}
		}

		return false;
	}

	private void handleWaterMovement() {
		dummyEntity.motionX = motionX;
		dummyEntity.motionY = motionY;
		dummyEntity.motionZ = motionZ;

		if (this.world.handleMaterialAcceleration(getBoundingBox().expand(0.0D, -0.4000000059604645D, 0.0D).contract(0.001D, 0.001D, 0.001D), Material.WATER, dummyEntity)) {

			motionX = dummyEntity.motionX;
			motionY = dummyEntity.motionY;
			motionZ = dummyEntity.motionZ;
		}
	}

	@Override
	public void move(double x, double y, double z) {
		double X = x;
		double Y = y;
		double Z = z;

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
		this.onGround = y != Y && Y < 0.0D;

		if (!FBP.lowTraction && !FBP.bounceOffWalls) {
			if (x != X)
				motionX *= 0.699999988079071D;
			if (z != Z)
				motionZ *= 0.699999988079071D;
		}
	}

	@Override
	public void renderParticle(BufferBuilder buf, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
		if (!FBP.isEnabled() && particleMaxAge != 0)
			particleMaxAge = 0;
		if (FBPKeyBindings.FBPSweep.isKeyDown() && !killToggle)
			killToggle = true;

		float f, f1, f2, f3;

		float f4 = (float) (prevParticleScale + (particleScale - prevParticleScale) * partialTicks);

		if (particleTexture != null) {
			f = particleTexture.getInterpolatedU(particleTextureJitterX / 4 * 16);
			f2 = particleTexture.getInterpolatedV(particleTextureJitterY / 4 * 16);

			f1 = particleTexture.getInterpolatedU((particleTextureJitterX + 1) / 4 * 16);
			f3 = particleTexture.getInterpolatedV((particleTextureJitterY + 1) / 4 * 16);
		} else {
			f = (particleTextureIndexX + particleTextureJitterX / 4) / 16;
			f1 = f + 0.015609375F;
			f2 = (particleTextureIndexY + particleTextureJitterY / 4) / 16;
			f3 = f2 + 0.015609375F;
		}

		float x = (float) (prevPosX + (posX - prevPosX) * partialTicks - interpPosX);
		float y = (float) (prevPosY + (posY - prevPosY) * partialTicks - interpPosY);
		float z = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks - interpPosZ);

		int i = getBrightnessForRender(partialTicks);

		par = new Vec2f[] { new Vec2f(f1, f3), new Vec2f(f1, f2), new Vec2f(f, f2), new Vec2f(f, f3) };

		float alpha = (float) (prevParticleAlpha + (particleAlpha - prevParticleAlpha) * partialTicks);

		if (FBP.restOnFloor)
			y += f4 / 10;

		FBPVector3d smoothRot = new FBPVector3d(0, 0, 0);

		if (FBP.rotationMult > 0) {
			smoothRot.y = rot.y;
			smoothRot.z = rot.z;

			if (!FBP.randomRotation)
				smoothRot.x = rot.x;

			// SMOOTH ROTATION
			if (!FBP.frozen) {
				FBPVector3d vec = rot.partialVec(prevRot, partialTicks);

				if (FBP.randomRotation) {
					smoothRot.y = vec.y;
					smoothRot.z = vec.z;
				} else {
					smoothRot.x = vec.x;
				}
			}
		}

		// RENDER
		FBPRenderer.renderCubeShaded_S(buf, par, x, y, z, f4 / 10, smoothRot, i >> 16 & 65535, i & 65535, particleRed, particleGreen, particleBlue, alpha);
	}

	private void createRotationMatrix() {
		double rx0 = FBP.random.nextDouble();
		double ry0 = FBP.random.nextDouble();
		double rz0 = FBP.random.nextDouble();

		rotStep = new FBPVector3d(rx0 > 0.5 ? 1 : -1, ry0 > 0.5 ? 1 : -1, rz0 > 0.5 ? 1 : -1);

		rot.copyFrom(rotStep);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float partialTicks) {
		AxisAlignedBB box = getBoundingBox();

		if (this.world.isBlockLoaded(new BlockPos(posX, 0, posZ))) {
			double d0 = (box.maxY - box.minY) * 0.66D;
			double k = this.posY + d0 + 0.01 - (FBP.restOnFloor ? particleScale / 10 : 0);
			return this.world.getCombinedLight(new BlockPos(posX, k, posZ), 0);
		} else {
			return 0;
		}
	}

	private void calculateYAngle()
	{
		double angleSin = Math.toDegrees(Math.asin(motionX / Math.sqrt(motionX * motionX + motionZ * motionZ)));

		if (motionZ > 0)
			rot.y = -angleSin;
		else
			rot.y = angleSin;
	}

	double getMult() {
		return Math.sqrt(motionX * motionX + motionZ * motionZ) * (FBP.randomRotation ? 200 : 500) * FBP.rotationMult;
	}
}