package io.mosip.mock.sbi.devicehelper.face;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mosip.mock.sbi.SBIConstant;
import io.mosip.mock.sbi.devicehelper.SBIDeviceHelper;
import io.mosip.mock.sbi.devicehelper.face.source.FaceFrameSource;
import io.mosip.mock.sbi.devicehelper.face.source.FaceFrameSources;
import io.mosip.mock.sbi.util.ApplicationPropertyHelper;
import io.mosip.mock.sbi.util.BioUtilHelper;
import io.mosip.mock.sbi.util.FaceIsoBuilder;
import io.mosip.mock.sbi.util.StringHelper;

public class SBIFaceHelper extends SBIDeviceHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(SBIFaceHelper.class);	

	// Null in static mode: fixed stream image and prepared ISO records
	private final FaceFrameSource frameSource;

	private SBIFaceHelper(int port, String purpose, String keystoreFilePath, String biometricImageType)
	{ 
		super (port, purpose, SBIConstant.MOSIP_BIOMETRIC_TYPE_FACE, SBIConstant.MOSIP_BIOMETRIC_SUBTYPE_FACE, keystoreFilePath, biometricImageType);
		this.frameSource = FaceFrameSources.create();
		if (frameSource != null)
		{
			try {
				frameSource.prepare();
			} catch (Exception ex) {
				LOGGER.error("Face frame source not available", ex);
			}
		}
	} 
  
	//synchronized method to control simultaneous access 
	synchronized public static SBIFaceHelper getInstance(int port, String purpose, String keystoreFilePath, String biometricImageType)
	{ 
		return new SBIFaceHelper(port, purpose, keystoreFilePath, biometricImageType);
	}

	@Override
	public long initDevice() {
		SBIFaceCaptureInfo captureInfo = new SBIFaceCaptureInfo ();
		captureInfo.initCaptureInfo();
		setCaptureInfo (captureInfo);
		// Loads the liveness models before the first stream rather than during it.
		FacePassiveScorer.get();
		if (frameSource != null)
		{
			try {
				frameSource.open();
			} catch (Exception ex) {
				LOGGER.error("initDevice :: frame source cannot be opened", ex);
				return -1;
			}
		}
		return 0;
	}

	@Override
	public int deInitDevice() {
		if (getCaptureInfo () != null)
			getCaptureInfo ().deInitCaptureInfo ();
		setCaptureInfo (null);
		if (frameSource != null)
			frameSource.close();
		return 0;
	}

	@Override
	public int getLiveStream() {
		byte [] image = frameSource != null ? frameSource.nextFrame() : getLiveStreamBufferedImage();
		if (image == null || image.length == 0)
			return -1;
		getCaptureInfo ().setImage(image);
		// Passive liveness score of the frame, sent with it in the stream.
		FacePassiveScorer scorer = FacePassiveScorer.get();
		((SBIFaceCaptureInfo) getCaptureInfo ()).setLivenessScore(scorer != null ? scorer.score(image) : null);
		
		return 0;
	}

	@Override
	public int getBioCapture(boolean isUsedForAuthenication) throws Exception {
		// An unplugged replay gives no capture: the request ends with the timeout error of the device.
		if (frameSource != null && !frameSource.available())
			return -1;
		byte [] isoData = null;

		String seedName = "";
		if (this.getProfileId().equalsIgnoreCase(SBIConstant.PROFILE_AUTOMATIC))
		{
			int seedValue = -1;
			if (this.getPurpose().equalsIgnoreCase(SBIConstant.PURPOSE_AUTH))
			{
				if (ApplicationPropertyHelper.getPropertyKeyValue(SBIConstant.MOSIP_BIOMETRIC_AUTH_SEED_FACE) != null)
				{
					seedValue = Integer.parseInt(ApplicationPropertyHelper.getPropertyKeyValue(SBIConstant.MOSIP_BIOMETRIC_AUTH_SEED_FACE));
					seedName = String.format("%04d", getRandomNumberForSeed(seedValue)).trim();
				}
			}
			else if (this.getPurpose().equalsIgnoreCase(SBIConstant.PURPOSE_REGISTRATION))
			{
				if (ApplicationPropertyHelper.getPropertyKeyValue(SBIConstant.MOSIP_BIOMETRIC_REGISTRATION_SEED_FACE) != null)
				{
					seedValue = Integer.parseInt(ApplicationPropertyHelper.getPropertyKeyValue(SBIConstant.MOSIP_BIOMETRIC_REGISTRATION_SEED_FACE));
					seedName = String.format("%04d", getRandomNumberForSeed(seedValue)).trim();
				}
			}
		}

		if (!isUsedForAuthenication)
		{
			isoData = frameSource != null ? getFaceISOFromFrame() : getBiometricISOImage(seedName, SBIConstant.PROFILE_BIO_FILE_NAME_FACE);
			if (isoData != null && !((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureFace())
			{
				((SBIFaceCaptureInfo)getCaptureInfo ()).setBioValueFace(StringHelper.base64UrlEncode(isoData));
				if (isScoreFromIso())
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(BioUtilHelper.getFaceQualityScoreFromIso(getPurpose(), isoData));
				else
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(getQualityScore());
			}				
			((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureFace(true);				
			
			isoData = getBiometricISOImage(seedName, SBIConstant.PROFILE_BIO_FILE_NAME_FACE_EXCEPTION);
			if (isoData != null && !((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureExceptionPhoto())
			{
				((SBIFaceCaptureInfo)getCaptureInfo ()).setBioValueExceptionPhoto(StringHelper.base64UrlEncode(isoData));
				if (isScoreFromIso())
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(BioUtilHelper.getFaceQualityScoreFromIso(getPurpose(), isoData));
				else
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(getQualityScore());
			}				
			((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureExceptionPhoto(true);				

			if (((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureFace() ||
					((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureExceptionPhoto())
			{
				getCaptureInfo ().setCaptureCompleted(true);
			}
		}
		else
		{
			isoData = getBiometricISOImage(seedName, SBIConstant.PROFILE_BIO_FILE_NAME_FACE);
			if (isoData != null && !((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureFace())
			{
				getCaptureInfo ().addBiometricForBioSubType(SBIConstant.BIO_NAME_UNKNOWN, StringHelper.base64UrlEncode(isoData));
				if (isScoreFromIso())
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(BioUtilHelper.getFaceQualityScoreFromIso(getPurpose(), isoData));
				else
					((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureScoreFace(getQualityScore());
				((SBIFaceCaptureInfo)getCaptureInfo ()).setCaptureFace(true);				
			}	
			
			if (((SBIFaceCaptureInfo)getCaptureInfo ()).isCaptureFace())
				getCaptureInfo ().setCaptureCompleted(true);
		}
		return 0;
	} 	

	// Uses the last streamed frame, or reads one when RCAPTURE comes without STREAM
	private byte[] getFaceISOFromFrame() throws Exception {
		byte [] frame = getCaptureInfo ().getImage();
		if (frame == null || frame.length == 0)
			frame = frameSource.nextFrame();
		if (frame == null || frame.length == 0)
			return null;
		return FaceIsoBuilder.fromJpeg(getPurpose(), frame);
	}
}