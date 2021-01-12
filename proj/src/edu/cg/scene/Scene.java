package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.algebra.*;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; // gets the values of 1, 2 and 3
	private boolean renderRefarctions = false;
	private boolean renderReflections = false;

	private PinholeCamera camera;
	private Vec ambient = new Vec(1, 1, 1); // white
	private Vec backgroundColor = new Vec(0, 0.5, 1); // blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();

	// MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec, distanceToPlain);
		return this;
	}

	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}

	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}

	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}

	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}

	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}

	public Scene initName(String name) {
		this.name = name;
		return this;
	}

	public Scene initRenderRefarctions(boolean renderRefarctions) {
		this.renderRefarctions = renderRefarctions;
		return this;
	}

	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}

	// MARK: getters
	public String getName() {
		return name;
	}

	public int getFactor() {
		return antiAliasingFactor;
	}

	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}

	public boolean getRenderRefarctions() {
		return renderRefarctions;
	}

	public boolean getRenderReflections() {
		return renderReflections;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Camera: " + camera + endl + "Ambient: " + ambient + endl + "Background Color: " + backgroundColor + endl
				+ "Max recursion level: " + maxRecursionLevel + endl + "Anti aliasing factor: " + antiAliasingFactor
				+ endl + "Light sources:" + endl + lightSources + endl + "Surfaces:" + endl + surfaces;
	}

	private transient ExecutorService executor = null;
	private transient Logger logger = null;

	private void initSomeFields(int imgWidth, int imgHeight, Logger logger) {
		this.logger = logger;
		// TODO: initialize your additional field here.
	}

	public BufferedImage render(int imgWidth, int imgHeight, double viewAngle, Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {

		initSomeFields(imgWidth, imgHeight, logger);

		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, viewAngle);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Intitialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);

		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][]) (new Future[imgHeight][imgWidth]);

		this.logger.log("Starting to shoot " + (imgHeight * imgWidth * antiAliasingFactor * antiAliasingFactor)
				+ " rays over " + name);

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);

		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}

		executor.shutdown();

		this.logger.log("Ray tracing of " + name + " has been completed.");

		executor = null;
		this.logger = null;

		return img;
	}

	private Future<Color> calcColor(int x, int y) {
		return executor.submit(() -> {
			// TODO: You need to re-implement this method if you want to handle
			// super-sampling. You're also free to change the given implementation if you
			// want.
			Point centerPoint = camera.transform(x, y);
			Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
			Vec color = calcColor(ray, 0);
			return color.toColor();
		});
	}

	// ------------------ helpers ------------------

	private Vec specular(Hit hit, Ray rayLight, Ray rayViewer) {
		Vec normal = hit.getNormalToSurface();
		Vec direction = rayLight.direction();
		Vec reflectionNormal = Ops.reflect(direction.neg(), normal);
		Vec ks = hit.getSurface().Ks();
		Vec v = rayViewer.direction();
		int shi = hit.getSurface().shininess();
		double cos = reflectionNormal.dot(v.neg());
		if (cos < 0){
			return new Vec();
		}
		return ks.mult(Math.pow(cos, shi));
	}

	private boolean occluded(Light light, Ray rayLight) {
		for (int i = 0; i < this.surfaces.size(); i++) {
			Surface surface = this.surfaces.get(i);
			if (light.isOccludedBy(surface, rayLight)) {
				return true;
			}
		}
		return false;
	}

	private Vec diffuse(Hit hit, Ray rayLight) {
		Vec normal = hit.getNormalToSurface();
		Vec direction = rayLight.direction();
		Vec kd = hit.getSurface().Kd();
		return kd.mult(Math.max(normal.dot(direction), 0));
	}

	private Hit minHit(Ray ray) {
		Hit min = null;
		for (int i = 0; i < this.surfaces.size(); i++) {
			Surface surface = this.surfaces.get(i);
			Hit hit = surface.intersect(ray);
			if (min == null || (hit != null && min.compareTo(hit) > 0)) {
				min = hit;
			}
		}
		return min;
	}

	private Vec putLightSources(Vec color, Point hit, Hit min, Ray ray) {
		for (int i = 0; i < this.lightSources.size(); i++) {
			Light light = this.lightSources.get(i);
			Ray rayLight = light.rayToLight(hit);
			if (!this.occluded(light, rayLight)) {
				Vec diffuseColor = this.diffuse(min, rayLight);
				diffuseColor = diffuseColor.add(this.specular(min, rayLight, ray));
				Vec intensity = light.intensity(hit, rayLight);
				color = color.add(diffuseColor.mult(intensity));
			}
		}

		return color;
	}

	// ------------- done helpers ----------------


	private Vec calcColor(Ray ray, int recusionLevel) {
		if (recusionLevel >= this.maxRecursionLevel) {
			return new Vec();
		}
		Hit min = this.minHit(ray);
		if (min == null) {
			return this.backgroundColor;
		}

		Surface surface = min.getSurface();
		Point hit = ray.getHittingPoint(min);

		Vec color = surface.Ka().mult(this.ambient);

		color = putLightSources(color, hit, min, ray);

		if (this.renderReflections) {
			Vec refDir = Ops.reflect(ray.direction(), min.getNormalToSurface());
			Vec refW = new Vec(surface.reflectionIntensity());
			Vec refColor = this.calcColor(new Ray(hit, refDir), recusionLevel + 1).mult(refW);
			color = color.add(refColor);
		}
		if (this.renderRefarctions) {
			if (surface.isTransparent()) {
				double n1 = surface.n1(min);
				double n2 = surface.n2(min);
				Vec refDir = Ops.refract(ray.direction(), min.getNormalToSurface(), n1, n2);
				Vec refW = new Vec(surface.refractionIntensity());
				Vec refColor = this.calcColor(new Ray(hit, refDir), recusionLevel + 1).mult(refW);
				color = color.add(refColor);
			}
		}
		return color;
	}

}