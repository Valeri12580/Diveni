<template>
  <div
    :style="`background-color: ${member.hexColor};`"
    class="session-member-card text-center card m-2"
    :class="{ greyOut: !props.highlight && props.estimateFinished }"
  >
    <h1 class="fs-3-rem">
      <strong v-if="!props.estimateFinished">?</strong>
      <strong v-if="member.currentEstimation === null && props.estimateFinished">-</strong>
      <strong v-if="member.currentEstimation !== null && props.estimateFinished">{{
        member.currentEstimation
      }}</strong>
    </h1>
    <rounded-avatar :member="member" :admin="false" />
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import RoundedAvatar from "@/components/RoundedAvatar.vue";

export default Vue.extend({
  name: "SessionMemberCard",
  components: { RoundedAvatar },
  props: {
    member: { type: Object, required: true },
    props: {
      type: Object,
      required: false,
      default: () => ({ estimateFinished: false, highlight: false }),
    },
  },
});
</script>

<!-- Add "scoped" attribute to limit CSS/SCSS to this component only -->
<style lang="scss" scoped>
.fs-3-rem {
  font-size: 4rem;
  color: black;
}
.card {
  position: relative;
  width: 168px;
  height: 222px;
  justify-content: center; /* Centering y-axis */
  align-items: center; /* Centering x-axis */
  border-radius: 5%;
  display: flex;
  flex-direction: column;
}
h4 {
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  width: 100px;
}
.greyOut {
  opacity: 0.5;
  transform: scale(0.8);
  /*border-width: 5px;*/
  /*border-color: darkred;*/
}
</style>
