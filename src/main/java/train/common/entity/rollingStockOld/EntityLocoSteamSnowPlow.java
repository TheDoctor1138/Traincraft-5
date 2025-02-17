package train.common.entity.rollingStockOld;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import train.common.Traincraft;
import train.common.api.LiquidManager;
import train.common.api.SteamTrain;
import train.common.core.FakePlayer;
import train.common.core.util.TraincraftUtil;
import train.common.library.GuiIDs;

import java.util.Random;

public class EntityLocoSteamSnowPlow extends SteamTrain {
	public EntityLocoSteamSnowPlow(World world) {
		super(world, LiquidManager.WATER_FILTER);
	}

	public EntityLocoSteamSnowPlow(World world, double d, double d1, double d2) {
		this(world);
		setPosition(d, d1 + yOffset, d2);
		motionX = 0.0D;
		motionY = 0.0D;
		motionZ = 0.0D;
		prevPosX = d;
		prevPosY = d1;
		prevPosZ = d2;
	}

	@Override
	public void updateRiderPosition() {
		if(riddenByEntity==null){return;}
		double pitchRads = this.anglePitchClient * Math.PI / 180.0D;
		double distance = 0.7;
		double yOffset = 0.3;
		float rotationCos1 = (float) Math.cos(Math.toRadians(this.renderYaw + 90));
		float rotationSin1 = (float) Math.sin(Math.toRadians((this.renderYaw + 90)));
		if(side.isServer()){
			rotationCos1 = (float) Math.cos(Math.toRadians(this.serverRealRotation + 90));
			rotationSin1 = (float) Math.sin(Math.toRadians((this.serverRealRotation + 90)));
			anglePitchClient = serverRealPitch*60;
		}
		float pitch = (float) (posY + ((Math.tan(pitchRads) * distance) + getMountedYOffset())
				+ riddenByEntity.getYOffset() + yOffset);
		float pitch1 = (float) (posY + getMountedYOffset() + riddenByEntity.getYOffset() + yOffset);
		double bogieX1 = (this.posX + (rotationCos1 * distance));
		double bogieZ1 = (this.posZ + (rotationSin1* distance));
		// System.out.println(rotationCos1+" "+rotationSin1);
		if (anglePitchClient > 20 && rotationCos1 == 1) {
			bogieX1-=pitchRads*2;
			pitch -= pitchRads * 1.2;
		}
		if (anglePitchClient > 20 && rotationSin1 == 1) {
			bogieZ1-=pitchRads*2;
			pitch -= pitchRads * 1.2;
		}
		if (pitchRads == 0.0) {
			riddenByEntity.setPosition(bogieX1, pitch1, bogieZ1);
		}
		if (pitchRads > -1.01 && pitchRads < 1.01) {
			riddenByEntity.setPosition(bogieX1, pitch, bogieZ1);
		}
	}

	@Override
	public void setDead() {
		super.setDead();
		isDead = true;
	}

	@Override
	public void pressKey(int i) {
		if (i == 7 && riddenByEntity != null && riddenByEntity instanceof EntityPlayer) {
			((EntityPlayer) riddenByEntity).openGui(Traincraft.instance, GuiIDs.LOCO, worldObj, (int) this.posX, (int) this.posY, (int) this.posZ);
		}
	}

	private static final double[][]	blockpos	= { { 4, 0, 1 }, { 4, 0, -1 }, { 4, 0, 0 }};
	private double[] point1;
	private FakePlayer fakePlayer = null;
	private int rotation =0;

	private static final float radianF = (float) Math.PI / 180.0f;
	@Override
	public void onUpdate() {
		super.onUpdate();
		if (worldObj.isRemote || bogieFront==null || bogieBack==null) {
			return;
		}
		checkInvent(cargoItems[0], cargoItems[1], this);
		if (fakePlayer == null){
			 fakePlayer = new FakePlayer(worldObj);
		}
		rotation = MathHelper.floor_float(TraincraftUtil.atan2degreesf(
				bogieFront.posZ - bogieBack.posZ,
				bogieFront.posX - bogieBack.posX));

		point1 = rotateVec3(blockpos[0], getPitch(), rotation);
		point1[0] += posX;point1[1] += posY;point1[2] += posZ;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);
		point1[1]++;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);
		point1[1]++;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);


		point1 = rotateVec3(blockpos[1], getPitch(), rotation);
		point1[0] += posX;point1[1] += posY;point1[2] += posZ;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);
		point1[1]++;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);
		point1[1]++;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);


		point1 = rotateVec3(blockpos[2], getPitch(), rotation);
		point1[0] += posX;point1[1] += posY+1;point1[2] += posZ;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);
		point1[1]++;
		mineSnow(worldObj, point1, cargoItems, fakePlayer);

	}

	private static void mineSnow(World worldObj, double[] point, ItemStack[] cargoItems, FakePlayer fakePlayer){
		Block b = worldObj.getBlock(MathHelper.floor_double(point[0]),MathHelper.floor_double(point[1]),MathHelper.floor_double(point[2]));
		int blockMeta = worldObj.getBlockMetadata(MathHelper.floor_double(point[0]), MathHelper.floor_double(point[1]),
				MathHelper.floor_double(point[2]));

		if((b == Blocks.snow || b == Blocks.snow_layer) && b.canHarvestBlock(fakePlayer, blockMeta)){
			worldObj.setBlockToAir(MathHelper.floor_double(point[0]),MathHelper.floor_double(point[1]),MathHelper.floor_double(point[2]));
			int snowballs = new Random().nextInt(9);
			for(int i=2; i<cargoItems.length && snowballs>0; i++){
				if (cargoItems[i] == null){
					cargoItems[i] = new ItemStack(Items.snowball, snowballs);
					snowballs--;
				} else if (cargoItems[i].getItem() == Items.snowball && cargoItems[i].stackSize < Items.snowball.getItemStackLimit()){
					while (cargoItems[i].stackSize < cargoItems[i].getMaxStackSize() && snowballs >0){
						cargoItems[i].stackSize++;
						snowballs--;
					}
				}
				if (snowballs ==0){
					break;
				}
			}
			if (snowballs >0){
				EntityItem entityitem = new EntityItem(worldObj, point[0], point[1] + 1, point[2], new ItemStack(Items.snowball, snowballs));
				entityitem.delayBeforeCanPickup = 10;
				worldObj.spawnEntityInWorld(entityitem);

			}
		}
	}

	private static double[] rotateVec3(double[] offset, float pitch, float yaw) {
		double[] xyz = new double[]{offset[0],offset[1],offset[2]};
		//rotate pitch
		if (pitch != 0.0F) {
			pitch *= radianF;

			xyz[0] = (offset[0] * Math.cos(pitch));
			xyz[1] = (offset[0] * Math.sin(pitch));
		}
		//rotate yaw
		if (yaw != 0.0F) {
			yaw *= radianF;
			double cos = MathHelper.cos(yaw);
			double sin = MathHelper.sin(yaw);

			xyz[0] = (offset[0] * cos) - (offset[2] * sin);
			xyz[2] = (offset[0] * sin) + (offset[2] * cos);
		}
		return xyz;
	}

	@Override
	public String getInventoryName() {
		return "Steam Snow Plow";
	}

	@Override
	public boolean interactFirst(EntityPlayer entityplayer) {
		playerEntity = entityplayer;
		if ((super.interactFirst(entityplayer))) {
			return false;
		}
		if (!worldObj.isRemote) {
			if (riddenByEntity != null && (riddenByEntity instanceof EntityPlayer) && riddenByEntity != entityplayer) {
				return true;
			}
			entityplayer.mountEntity(this);
		}
		return true;
	}

	@Override
	public float getOptimalDistance(EntityMinecart cart) {
		return (0.7F);
	}
	@Override
	public boolean canBeAdjusted(EntityMinecart cart) {
		return canBeAdjusted;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}
}