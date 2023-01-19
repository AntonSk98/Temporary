package main;

import modicio.core.*;
import modicio.core.rules.api.AttributeRuleJ;
import modicio.nativelang.defaults.SimpleDefinitionVerifier;
import modicio.nativelang.defaults.SimpleMapRegistry;
import modicio.nativelang.defaults.SimpleModelVerifier;
import modicio.verification.DefinitionVerifier;
import modicio.verification.ModelVerifier;
import scala.Option;
import scala.Some;
import scala.concurrent.Future;
import scala.jdk.javaapi.FutureConverters;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class MainClass {

    private static DefinitionVerifier definitionVerifier;
    private static ModelVerifier modelVerifier;
    private static TypeFactory typeFactory;

    private static InstanceFactory instanceFactory;
    private static Registry registry;

    public static void main(String[] args) {
        try {
            sample();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sample() throws ExecutionException, InterruptedException {
        setUpModicio();
        createRootElement();
        createProjectType();
        createProjectInstance();
    }

    private static void createProjectInstance() throws ExecutionException, InterruptedException {
        // create Instance of a Project
        String createdInstanceId = future(instanceFactory.newInstance("Project")).get().getInstanceId();
        DeepInstance deepInstance = future(registry.get(createdInstanceId)).get().get();
        future(deepInstance.unfold()).get();
        deepInstance.assignDeepValue("name", "Local attribute - will not be saved");
        deepInstance.assignDeepValue("responsible", "Local attribute - will not be saved");
        deepInstance.assignDeepValue("started", "Local attribute - will not be saved");
        //deepInstance.commitJ().get();

        System.out.println("Attribute values updated...");

        //checking it
        DeepInstance deepInstanceJ = future(registry.get(createdInstanceId)).get().get();
        future(deepInstanceJ.unfold()).get();
        var attributeValuesMap = deepInstanceJ.getDeepAttributes();
        System.out.println(attributeValuesMap);
    }

    private static void setUpModicio() {
        definitionVerifier = new SimpleDefinitionVerifier();
        modelVerifier = new SimpleModelVerifier();
        typeFactory = new TypeFactory(definitionVerifier, modelVerifier);
        instanceFactory = new InstanceFactory(definitionVerifier, modelVerifier);
        registry = new SimpleMapRegistry(typeFactory, instanceFactory);

        typeFactory.setRegistry(registry);
        instanceFactory.setRegistry(registry);
    }

    private static void createRootElement() throws ExecutionException, InterruptedException {
        CompletableFuture<TypeHandle> root = future(typeFactory.newType(
                ModelElement.ROOT_NAME(),
                ModelElement.REFERENCE_IDENTITY(),
                true,
                new Some<>((TimeIdentity.create()))));
        CompletableFuture<Object> typeFuture = future(registry.setType(root.get(), false));
        typeFuture.get();
    }

    private static void createProjectType() throws ExecutionException, InterruptedException {
        CompletableFuture<TypeHandle> type = future(typeFactory.newType("Project", ModelElement.REFERENCE_IDENTITY(), false, Option.empty()));
        // enforce synchronous execution
        future(registry.setType(type.get(), false)).get();
        TypeHandle typeHandle = future(registry.getType("Project", ModelElement.REFERENCE_IDENTITY())).get().get();
        future(typeHandle.unfold()).get();
        typeHandle.applyRule(AttributeRuleJ.create("name", "String", false, Option.empty()));
        typeHandle.applyRule(AttributeRuleJ.create("responsible", "String", false, Option.empty()));
        typeHandle.applyRule(AttributeRuleJ.create("started", "String", false, Option.empty()));
    }

    private static  <T> CompletableFuture<T> future(Future<T> future) {
        return FutureConverters.asJava(future).toCompletableFuture();
    }

    private static  <T> Optional<T> toOptional(Option<T> option) {
        return OptionConverters.toJava(option);
    }

}
