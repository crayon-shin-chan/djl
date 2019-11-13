/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl;

import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.Translator;
import ai.djl.util.PairList;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * A model is a collection of artifacts that is created by the training process.
 *
 * <p>A deep learning model usually contains the following parts:
 *
 * <ul>
 *   <li>Graph: aka Symbols in MXNet, model in Keras, Block in Pytorch
 *   <li>Parameters: weights
 *   <li>Input/Output information: input and output parameter names, shape, etc.
 *   <li>Other artifacts: e.g. dictionary for classification
 * </ul>
 *
 * <p>In a common inference case, the model is usually loaded from a file. Once the model is loaded,
 * you can create a {@link Predictor} with the loaded model and call {@link
 * Predictor#predict(Object)} to get the inference result.
 *
 * <pre>
 * Model model = <b>Model.load</b>(modelDir, modelName);
 *
 * // User must implement Translator interface, read Translator for detail.
 * Translator translator = new MyTranslator();
 *
 * try (Predictor&lt;String, String&gt; predictor = <b>model.newPredictor</b>(translator)) {
 *   String result = predictor.<b>predict</b>("What's up");
 * }
 * </pre>
 *
 * @see Model#load(Path, String)
 * @see Predictor
 * @see Translator
 */
public interface Model extends AutoCloseable {

    /**
     * Creates an empty model instance.
     *
     * @return a new Model instance
     */
    static Model newInstance() {
        return newInstance(Device.defaultDevice());
    }

    /**
     * Creates an empty model instance on the specified {@link Device}.
     *
     * @param device the device to load the model onto
     * @return a new model instance
     */
    static Model newInstance(Device device) {
        return Engine.getInstance().newModel(device);
    }

    /**
     * Loads the model from the {@code modelPath}.
     *
     * @param modelPath the directory or file path of the model location
     * @throws IOException when IO operation fails in loading a resource
     * @throws MalformedModelException if model file is corrupted
     */
    default void load(Path modelPath) throws IOException, MalformedModelException {
        load(modelPath, modelPath.toFile().getName(), null);
    }

    /**
     * Loads the model from the {@code modelPath} and the given name.
     *
     * @param modelPath the directory or file path of the model location
     * @param modelName the model file name
     * @throws IOException when IO operation fails in loading a resource
     * @throws MalformedModelException if model file is corrupted
     */
    default void load(Path modelPath, String modelName)
            throws IOException, MalformedModelException {
        load(modelPath, modelName, null);
    }

    /**
     * Loads the model from the {@code modelPath} with the name and options provided.
     *
     * @param modelPath the directory or file path of the model location
     * @param modelName the model file name
     * @param options engine specific load model options, see documentation for each engine
     * @throws IOException when IO operation fails in loading a resource
     * @throws MalformedModelException if model file is corrupted
     */
    void load(Path modelPath, String modelName, Map<String, String> options)
            throws IOException, MalformedModelException;

    /**
     * Saves the model to the specified {@code modelPath} with the name provided.
     *
     * @param modelPath the directory or file path of the model location
     * @param modelName the model file name
     * @throws IOException when IO operation fails in loading a resource
     */
    void save(Path modelPath, String modelName) throws IOException;

    /**
     * Gets the block from the Model.
     *
     * @return the {@link Block}
     */
    Block getBlock();

    /**
     * Sets the block for the Model for training and inference.
     *
     * @param block the {@link Block} used in Model
     */
    void setBlock(Block block);

    /**
     * Gets the model name.
     *
     * @return name of the model
     */
    String getName();

    /**
     * Gets the property of the model based on property name.
     *
     * @param key the name of the property
     * @return the value of the property
     */
    String getProperty(String key);

    /**
     * Sets a property to the model.
     *
     * <p>properties will be saved/loaded with model, user can store some information about the
     * model in here.
     *
     * @param key the name of the property
     * @param value the value of the property
     */
    void setProperty(String key, String value);

    /**
     * Gets the NDArray Manager from the model.
     *
     * @return the {@link NDManager}
     */
    NDManager getNDManager();

    /**
     * Creates a new {@link Trainer} instance for a Model.
     *
     * @param trainingConfig training configuration settings
     * @return the {@link Trainer} instance
     */
    Trainer newTrainer(TrainingConfig trainingConfig);

    /**
     * Creates a new Predictor based on the model.
     *
     * @param translator the object used for pre-processing and postprocessing
     * @param <I> the input object for pre-processing
     * @param <O> the output object from postprocessing
     * @return an instance of {@code Predictor}
     */
    <I, O> Predictor<I, O> newPredictor(Translator<I, O> translator);

    /**
     * Returns the input descriptor of the model.
     *
     * <p>It contains the information that can be extracted from the model, usually name, shape,
     * layout and DataType.
     *
     * @return a PairList of String and Shape
     */
    PairList<String, Shape> describeInput();

    /**
     * Returns the output descriptor of the model.
     *
     * <p>It contains the output information that can be obtained from the model.
     *
     * @return a PairList of String and Shape
     */
    PairList<String, Shape> describeOutput();

    /**
     * Returns the artifact names associated with the model.
     *
     * @return an array of artifact names
     */
    String[] getArtifactNames();

    /**
     * Attempts to load the artifact using the given function and cache it if the specified artifact
     * is not already cached.
     *
     * <p>Model will cache loaded artifact, so the user doesn't need to keep tracking it.
     *
     * <pre>{@code
     * String synset = model.getArtifact("synset.txt", k -> IOUtils.toString(k)));
     * }</pre>
     *
     * @param name the name of the desired artifact
     * @param function the function to load the artifact
     * @param <T> the type of the returned artifact object
     * @return the current (existing or computed) artifact associated with the specified name, or
     *     null if the computed value is null
     * @throws IOException when IO operation fails in loading a resource
     * @throws ClassCastException if the cached artifact cannot be cast to the target class
     */
    <T> T getArtifact(String name, Function<InputStream, T> function) throws IOException;

    /**
     * Finds an artifact resource with a given name in the model.
     *
     * @param name the name of the desired artifact
     * @return a {@link java.net.URL} object or {@code null} if no artifact with this name is found
     * @throws IOException when IO operation fails in loading a resource
     */
    URL getArtifact(String name) throws IOException;

    /**
     * Finds an artifact resource with a given name in the model.
     *
     * @param name the name of the desired artifact
     * @return a {@link java.io.InputStream} object or {@code null} if no resource with this name is
     *     found
     * @throws IOException when IO operation fails in loading a resource
     */
    InputStream getArtifactAsStream(String name) throws IOException;

    void setDataType(DataType dataType);

    DataType getDataType();

    /**
     * Casts the model to support a different precision level.
     *
     * <p>For example, you can cast the precision from Float to Int
     *
     * @param dataType the target dataType you would like to cast to
     */
    void cast(DataType dataType);

    /** {@inheritDoc} */
    @Override
    void close();
}
