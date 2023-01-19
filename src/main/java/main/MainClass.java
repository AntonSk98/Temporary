package main;

import modicio.core.ModelElement;
import modicio.core.TimeIdentity;
import modicio.core.TypeHandle;
import modicio.core.api.*;
import modicio.core.rules.api.AttributeRuleJ;
import modicio.nativelang.defaults.api.SimpleDefinitionVerifierJ;
import modicio.nativelang.defaults.api.SimpleMapRegistryJ;
import modicio.nativelang.defaults.api.SimpleModelVerifierJ;
import modicio.verification.api.DefinitionVerifierJ;
import modicio.verification.api.ModelVerifierJ;
import scala.Option;
import scala.concurrent.Future;
import scala.jdk.javaapi.FutureConverters;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainClass {

    private static DefinitionVerifierJ definitionVerifierJ;
    private static ModelVerifierJ modelVerifierJ;
    private static TypeFactoryJ typeFactoryJ;

    private static InstanceFactoryJ instanceFactoryJ;
    private static RegistryJ registryJ;

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
        String createdInstanceId = instanceFactoryJ.newInstanceJ("Project").get().getInstanceIdJ();
        DeepInstanceJ deepInstance = registryJ.getJ(createdInstanceId).get().get();
        deepInstance.unfoldJ().get();
        deepInstance.assignDeepValueJ("name", "Local attribute - will not be saved");
        deepInstance.assignDeepValueJ("responsible", "Local attribute - will not be saved");
        deepInstance.assignDeepValueJ("started", "Local attribute - will not be saved");
        deepInstance.commitJ().get();

        System.out.println("Attribute values updated...");

        //checking it
        DeepInstanceJ deepInstanceJ = registryJ.getJ(createdInstanceId).get().get();
        deepInstanceJ.unfoldJ().get();
        var attributeValuesMap = deepInstanceJ.getDeepAttributes();
        System.out.println(attributeValuesMap);
    }

    private static void setUpModicio() {
        definitionVerifierJ = new SimpleDefinitionVerifierJ();
        modelVerifierJ = new SimpleModelVerifierJ();
        typeFactoryJ = new TypeFactoryJ(definitionVerifierJ, modelVerifierJ);
        instanceFactoryJ = new InstanceFactoryJ(definitionVerifierJ, modelVerifierJ);
        registryJ = new SimpleMapRegistryJ(typeFactoryJ, instanceFactoryJ);

        typeFactoryJ.setRegistryJ(registryJ);
        instanceFactoryJ.setRegistryJ(registryJ);
    }

    private static void createRootElement() throws ExecutionException, InterruptedException {
        CompletableFuture<TypeHandleJ> root = typeFactoryJ.newTypeJ(
                ModelElement.ROOT_NAME(),
                ModelElement.REFERENCE_IDENTITY(),
                true,
                Optional.of(TimeIdentity.create()));
        CompletableFuture<Object> typeFuture = registryJ.setType(root.get());
        typeFuture.get();
    }

    private static void createProjectType() throws ExecutionException, InterruptedException {
        CompletableFuture<TypeHandleJ> type = typeFactoryJ.newTypeJ("Project", ModelElement.REFERENCE_IDENTITY(), false);
        // enforce synchronous execution
        registryJ.setType(type.get()).get();
        TypeHandle typeHandle = toCompletableFuture(registryJ.getRegistry().getType("Project", ModelElement.REFERENCE_IDENTITY())).get().get();
        toCompletableFuture(typeHandle.unfold()).get();
        typeHandle.applyRule(AttributeRuleJ.create("name", "String", false, Option.empty()));
        typeHandle.applyRule(AttributeRuleJ.create("responsible", "String", false, Option.empty()));
        typeHandle.applyRule(AttributeRuleJ.create("started", "String", false, Option.empty()));
    }

    private static  <T> CompletableFuture<T> toCompletableFuture(Future<T> future) {
        return FutureConverters.asJava(future).toCompletableFuture();
    }

    private static  <T> Optional<T> toOptional(Option<T> option) {
        return OptionConverters.toJava(option);
    }

}
