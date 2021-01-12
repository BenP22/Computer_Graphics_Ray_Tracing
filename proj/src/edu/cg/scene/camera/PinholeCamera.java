package edu.cg.scene.camera;

import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;

public class PinholeCamera {
	Point cameraPosition;
	Point centerPixel;

	Vec towardsVec;
	Vec upVec;
	Vec rightVec;

	int height;
	int width;

	double viewAngle;
	double distanceToPlain;

	/**
	 * Initializes a pinhole camera model with default resolution 200X200 (RxXRy)
	 * and View Angle 90.
	 *
	 * @param cameraPosition  - The position of the camera.
	 * @param towardsVec      - The towards vector of the camera (not necessarily
	 *                        normalized).
	 * @param upVec           - The up vector of the camera.
	 * @param distanceToPlain - The distance of the camera (position) to the center
	 *                        point of the image-plain.
	 *
	 */
	public PinholeCamera(Point cameraPosition, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.cameraPosition = cameraPosition;

		this.towardsVec = towardsVec.normalize();
		this.rightVec =this.towardsVec.cross(upVec).normalize();
		this.upVec = this.rightVec.cross(this.towardsVec).normalize();

		this.distanceToPlain = distanceToPlain;
		this.centerPixel = new Ray(cameraPosition, this.towardsVec).add(this.distanceToPlain);

		this.height = 200;
		this.width = 200;

		this.viewAngle = 90;
	}

	/**
	 * Initializes the resolution and width of the image.
	 *
	 * @param height    - the number of pixels in the y direction.
	 * @param width     - the number of pixels in the x direction.
	 * @param viewAngle - the view Angle.
	 */
	public void initResolution(int height, int width, double viewAngle) {
		this.height = height;
		this.width = width;
		this.viewAngle = viewAngle;
	}

	/**
	 * Transforms from pixel coordinates to the center point of the corresponding
	 * pixel in model coordinates.
	 *
	 * @param x - the pixel index in the x direction.
	 * @param y - the pixel index in the y direction.
	 * @return the middle point of the pixel (x,y) in the model coordinates.
	 */
	public Point transform(int x, int y) {
		double plainWidth
				= (2 * Math.tan(Math.toRadians(this.viewAngle / 2.0)) * this.distanceToPlain);

		double upDist = (double)(y - (this.height / 2)) * (plainWidth / this.height) * (-1);
		double rightDist = -(double)(x - (this.width / 2)) * (plainWidth / this.width);

		return this.centerPixel.add(this.upVec.mult(upDist)).add(this.rightVec.mult(rightDist));
	}

	/**
	 * Returns the camera position
	 *
	 * @return a new point representing the camera position.
	 */
	public Point getCameraPosition() {
		return new Point(cameraPosition.x, cameraPosition.y, cameraPosition.z);
	}
}
